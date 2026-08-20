package dev.learning.reports.domain;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class CourseReportCsv {
    private final Clock clock;

    public CourseReportCsv(Clock clock) {
        this.clock = clock;
    }

    public byte[] render(CourseDeliveryReport report) {
        Instant now = clock.instant();
        StringBuilder csv = new StringBuilder(
                "course_id,course_title,learner_id,learner_name,deadline,completed_at,delivery_status\n");
        for (CourseDeliveryReport.LearnerDelivery learner : report.learners()) {
            csv.append(cell(report.courseId())).append(',')
                    .append(cell(report.courseTitle())).append(',')
                    .append(cell(learner.learnerId())).append(',')
                    .append(cell(learner.learnerName())).append(',')
                    .append(learner.deadline()).append(',')
                    .append(learner.completedAt() == null ? "" : learner.completedAt()).append(',')
                    .append(status(learner, now)).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String status(CourseDeliveryReport.LearnerDelivery learner, Instant now) {
        if (learner.completedAt() != null) {
            return learner.completedAt().isAfter(learner.deadline()) ? "COMPLETED_LATE" : "COMPLETED_ON_TIME";
        }
        return now.isAfter(learner.deadline()) ? "OVERDUE" : "IN_PROGRESS";
    }

    private String cell(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
