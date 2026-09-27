package com.onlineassessment.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class StudentDtos {
    private StudentDtos() {}

    public record Dashboard(
            long totalAssessmentsAttempted,
            double averageScore,
            double highestScore,
            double passPercentage,
            List<ResultDtos.Summary> recentResults,
            List<PerformancePoint> performance
    ) {}

    public record PerformancePoint(
            String assessment,
            double percentage,
            LocalDateTime submittedAt
    ) {}
}
