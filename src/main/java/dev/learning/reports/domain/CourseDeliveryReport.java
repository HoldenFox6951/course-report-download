package dev.learning.reports.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public record CourseDeliveryReport(
        @NotBlank String courseId,
        @NotBlank String courseTitle,
        @NotEmpty List<@Valid LearnerDelivery> learners) {

    public record LearnerDelivery(
            @NotBlank String learnerId,
            @NotBlank String learnerName,
            @NotNull Instant deadline,
            Instant completedAt) {
    }
}
