package dev.learning.reports.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.learning.reports.config.InfraiProperties;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class InfraiStorageClient {
    private static final int MAX_ATTEMPTS = 4;
    // Canonical endpoint shape: /v1/storage/object/presign/{bucket}/{key}
    private final HttpClient http;
    private final ObjectMapper json;
    private final InfraiProperties properties;

    public InfraiStorageClient(HttpClient http, ObjectMapper json, InfraiProperties properties) {
        this.http = http;
        this.json = json;
        this.properties = properties;
    }

    public void createReportBucket() {
        call("POST", "/v1/storage/bucket/create", Map.of("name", properties.storage().bucket()));
    }

    public String presignPut(String key, String requestId, int byteCount) {
        JsonNode data = call("POST", objectPath(key), Map.of(
                "op", "put",
                "expires_seconds", 300,
                "content_type", "text/csv; charset=utf-8",
                "max_bytes", byteCount,
                "idempotency_key", requestId + "-put"));
        return data.path("url").asText();
    }

    public String presignDownload(String key, String requestId) {
        JsonNode data = call("POST", objectPath(key), Map.of(
                "op", "get",
                "expires_seconds", properties.storage().downloadExpiresSeconds(),
                "response_disposition", "attachment; filename=\"" + key + "\"",
                "idempotency_key", requestId + "-get"));
        return data.path("url").asText();
    }

    public void upload(String url, byte[] csv) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "text/csv; charset=utf-8")
                .method("PUT", HttpRequest.BodyPublishers.ofByteArray(csv))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new InfraiException("UPLOAD_REJECTED", "CSV upload was rejected", response.statusCode());
            }
        } catch (IOException e) {
            throw new InfraiException("UPLOAD_TRANSPORT", e.getMessage(), 502);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InfraiException("UPLOAD_INTERRUPTED", "CSV upload was interrupted", 503);
        }
    }

    private String objectPath(String key) {
        return "/v1/storage/object/presign/" + segment(properties.storage().bucket()) + "/" + segment(key);
    }

    private String segment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private JsonNode call(String method, String path, Object body) {
        String payload;
        try {
            payload = json.writeValueAsString(body);
        } catch (IOException e) {
            throw new IllegalStateException("Could not encode request", e);
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.baseUrl() + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode envelope = json.readTree(response.body());
                if (!envelope.path("ok").asBoolean()) {
                    JsonNode error = envelope.path("error");
                    if (response.statusCode() == 429 && attempt + 1 < MAX_ATTEMPTS) {
                        pause(response, attempt);
                        continue;
                    }
                    throw new InfraiException(
                            error.path("code").asText("INFRAI_REJECTED"),
                            error.path("message").asText("Request rejected"),
                            response.statusCode());
                }
                return envelope.path("data");
            } catch (InfraiException e) {
                throw e;
            } catch (IOException e) {
                throw new InfraiException("INFRAI_TRANSPORT", e.getMessage(), 502);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InfraiException("INFRAI_INTERRUPTED", "Request was interrupted", 503);
            }
        }
        throw new IllegalStateException("Retry loop completed unexpectedly");
    }

    private void pause(HttpResponse<?> response, int attempt) throws InterruptedException {
        long seconds = response.headers().firstValue("Retry-After")
                .map(this::parseRetryAfter)
                .orElse(1L << attempt);
        Thread.sleep(Duration.ofSeconds(Math.min(seconds, 8)).toMillis());
    }

    private long parseRetryAfter(String value) {
        try {
            return Math.max(1, Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }
}
