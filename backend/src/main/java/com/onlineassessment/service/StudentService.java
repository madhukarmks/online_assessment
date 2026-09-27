package com.onlineassessment.service;

import com.onlineassessment.dto.ResultDtos;
import com.onlineassessment.dto.StudentDtos;
import com.onlineassessment.entity.User;
import com.onlineassessment.exception.Exceptions.NotFound;
import com.onlineassessment.repository.ResultRepository;
import com.onlineassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final UserRepository users;
    private final ResultRepository results;

    @Transactional(readOnly = true)
    public StudentDtos.Dashboard dashboard(String email) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new NotFound("User not found"));

        List<ResultDtos.Summary> summaries = results.findByUserIdOrderBySubmittedAtDesc(user.getId())
                .stream()
                .map(this::summary)
                .toList();

        double average = summaries.stream()
                .mapToDouble(ResultDtos.Summary::percentage)
                .average()
                .orElse(0.0);

        double highest = summaries.stream()
                .mapToDouble(ResultDtos.Summary::percentage)
                .max()
                .orElse(0.0);

        double passPercentage = summaries.isEmpty()
                ? 0.0
                : summaries.stream().filter(ResultDtos.Summary::passed).count() * 100.0 / summaries.size();

        List<StudentDtos.PerformancePoint> performance = summaries.stream()
                .limit(10)
                .sorted((a, b) -> {
                    if (a.submittedAt() == null) return 1;
                    if (b.submittedAt() == null) return -1;
                    return a.submittedAt().compareTo(b.submittedAt());
                })
                .map(r -> new StudentDtos.PerformancePoint(
                        r.assessmentTitle(),
                        r.percentage(),
                        r.submittedAt()
                ))
                .toList();

        return new StudentDtos.Dashboard(
                summaries.size(),
                average,
                highest,
                passPercentage,
                summaries.stream().limit(5).toList(),
                performance
        );
    }

    @Transactional(readOnly = true)
    public List<ResultDtos.Summary> history(String email) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new NotFound("User not found"));

        return results.findByUserIdOrderBySubmittedAtDesc(user.getId())
                .stream()
                .map(this::summary)
                .toList();
    }

    private ResultDtos.Summary summary(com.onlineassessment.entity.Result result) {
        return new ResultDtos.Summary(
                result.getId(),
                result.getAttempt().getId(),
                result.getAssessment().getTitle(),
                result.getUser().getName(),
                result.getTotalQuestions(),
                result.getAttemptedQuestions(),
                result.getCorrectAnswers(),
                result.getWrongAnswers(),
                result.getUnattemptedQuestions(),
                result.getTotalMarks(),
                result.getObtainedMarks(),
                result.getNegativeMarks(),
                result.getPercentage(),
                result.isPassed(),
                result.getTimeTakenSeconds(),
                result.getSubmittedAt()
        );
    }
}
