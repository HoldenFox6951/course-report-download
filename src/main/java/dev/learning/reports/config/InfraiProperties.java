package dev.learning.reports.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "infrai")
public record InfraiProperties(
        @NotBlank String baseUrl,
        @NotBlank String apiKey,
        @Valid Storage storage) {

    public record Storage(
            @NotBlank String bucket,
            @Positive int downloadExpiresSeconds) {
    }
}
