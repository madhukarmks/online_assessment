package com.onlineassessment.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class AdminDtos {
    private AdminDtos() {}

    public record Dashboard(
            long totalStudents,
            long totalAssessments,
            long totalQuestions,
            long totalAttempts,
            double averageScore,
            double passRate,
            List<AttemptRow> recentAttempts
    ) {}

    public record AttemptRow(
            Long attemptId,
            String student,
            String assessment,
            double score,
            double percentage,
            String status,
            LocalDateTime submittedAt
    ) {}

    public record StudentRow(
            Long id,
            String name,
            String email,
            boolean active,
            LocalDateTime createdAt,
            long attempts
    ) {}
}
