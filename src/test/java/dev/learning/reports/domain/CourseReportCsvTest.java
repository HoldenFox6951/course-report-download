package dev.learning.reports.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class CourseReportCsvTest {
    private final CourseReportCsv csv = new CourseReportCsv(
            Clock.fixed(Instant.parse("2026-08-21T10:00:00Z"), ZoneOffset.UTC));

    @Test
    void teachesEducatorsWhichLearnersNeedDeadlineFollowUp() {
        CourseDeliveryReport report = new CourseDeliveryReport(
                "course-101",
                "Writing for Research",
                List.of(
                        new CourseDeliveryReport.LearnerDelivery(
                                "learner-1", "Ava", Instant.parse("2026-08-20T09:00:00Z"), null),
                        new CourseDeliveryReport.LearnerDelivery(
                                "learner-2", "Mateo", Instant.parse("2026-08-22T09:00:00Z"),
                                Instant.parse("2026-08-21T08:00:00Z"))));

        String output = new String(csv.render(report), StandardCharsets.UTF_8);

        assertThat(output).contains("\"Ava\",2026-08-20T09:00:00Z,,OVERDUE");
        assertThat(output).contains("\"Mateo\",2026-08-22T09:00:00Z,2026-08-21T08:00:00Z,COMPLETED_ON_TIME");
    }
}
