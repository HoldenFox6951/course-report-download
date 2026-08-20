package dev.learning.reports.web;

import dev.learning.reports.domain.CourseDeliveryReport;
import dev.learning.reports.service.CourseReportExportService;
import dev.learning.reports.service.CourseReportExportService.ExportResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
public class CourseReportController {
    private final CourseReportExportService exports;

    public CourseReportController(CourseReportExportService exports) {
        this.exports = exports;
    }

    @PostMapping("/course-delivery")
    @ResponseStatus(HttpStatus.CREATED)
    public ExportResult create(@Valid @RequestBody CourseDeliveryReport report) {
        return exports.export(report);
    }
}
