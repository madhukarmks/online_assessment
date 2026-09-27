package com.onlineassessment.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public final class ResultDtos {
    private ResultDtos() {}

    public record Summary(
            Long id,
            Long attemptId,
            String assessmentTitle,
            String studentName,
            int totalQuestions,
            int attemptedQuestions,
            int correctAnswers,
            int wrongAnswers,
            int unattemptedQuestions,
            double totalMarks,
            double obtainedMarks,
            double negativeMarks,
            double percentage,
            boolean passed,
            long timeTakenSeconds,
            LocalDateTime submittedAt
    ) {}

    public record QuestionAnalysis(
            Long questionId,
            String question,
            Set<Long> studentAnswer,
            Set<Long> correctAnswer,
            String status,
            double marksObtained
    ) {}

    public record Detail(
            Summary summary,
            List<QuestionAnalysis> analysis
    ) {}
}
