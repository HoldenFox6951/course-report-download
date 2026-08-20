package dev.learning.reports.service;

import dev.learning.reports.domain.CourseDeliveryReport;
import dev.learning.reports.domain.CourseReportCsv;
import dev.learning.reports.storage.InfraiStorageClient;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CourseReportExportService {
    private final CourseReportCsv csv;
    private final InfraiStorageClient storage;
    private final Clock clock;

    public CourseReportExportService(CourseReportCsv csv, InfraiStorageClient storage, Clock clock) {
        this.csv = csv;
        this.storage = storage;
        this.clock = clock;
    }

    public ExportResult export(CourseDeliveryReport report) {
        byte[] bytes = csv.render(report);
        String requestId = UUID.randomUUID().toString();
        String key = safe(report.courseId()) + "-delivery-" + LocalDate.now(clock) + ".csv";

        storage.createReportBucket();
        storage.upload(storage.presignPut(key, requestId, bytes.length), bytes);
        String downloadUrl = storage.presignDownload(key, requestId);
        return new ExportResult(key, downloadUrl, bytes.length);
    }

    private String safe(String value) {
        String normalized = value.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        return normalized.replaceAll("(^-+|-+$)", "");
    }

    public record ExportResult(String objectKey, String downloadUrl, int bytes) {
    }
}
