package com.onlineassessment.service;

import com.onlineassessment.dto.AdminDtos;
import com.onlineassessment.entity.Result;
import com.onlineassessment.entity.Role;
import com.onlineassessment.entity.User;
import com.onlineassessment.exception.Exceptions.NotFound;
import com.onlineassessment.repository.AssessmentRepository;
import com.onlineassessment.repository.AttemptRepository;
import com.onlineassessment.repository.QuestionRepository;
import com.onlineassessment.repository.ResultRepository;
import com.onlineassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository users;
    private final AssessmentRepository assessments;
    private final QuestionRepository questions;
    private final AttemptRepository attempts;
    private final ResultRepository results;

    @Transactional(readOnly = true)
    public AdminDtos.Dashboard dashboard() {
        List<Result> allResults = results.findAll();

        double averageScore = allResults.stream()
                .mapToDouble(Result::getPercentage)
                .average()
                .orElse(0.0);

        double passRate = allResults.isEmpty()
                ? 0.0
                : allResults.stream().filter(Result::isPassed).count() * 100.0 / allResults.size();

        List<AdminDtos.AttemptRow> recent = attempts.findTop10ByOrderByStartedAtDesc()
                .stream()
                .map(attempt -> results.findByAttemptId(attempt.getId())
                        .map(result -> new AdminDtos.AttemptRow(
                                attempt.getId(),
                                attempt.getUser().getName(),
                                attempt.getAssessment().getTitle(),
                                result.getObtainedMarks(),
                                result.getPercentage(),
                                attempt.getStatus().name(),
                                attempt.getSubmittedAt()
                        ))
                        .orElseGet(() -> new AdminDtos.AttemptRow(
                                attempt.getId(),
                                attempt.getUser().getName(),
                                attempt.getAssessment().getTitle(),
                                0.0,
                                0.0,
                                attempt.getStatus().name(),
                                attempt.getSubmittedAt()
                        )))
                .toList();

        return new AdminDtos.Dashboard(
                users.countByRole(Role.STUDENT),
                assessments.count(),
                questions.count(),
                attempts.count(),
                averageScore,
                passRate,
                recent
        );
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.StudentRow> students(String search) {
        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        return users.findAll()
                .stream()
                .filter(user -> user.getRole() == Role.STUDENT)
                .filter(user -> term.isBlank()
                        || user.getName().toLowerCase(Locale.ROOT).contains(term)
                        || user.getEmail().toLowerCase(Locale.ROOT).contains(term))
                .map(user -> new AdminDtos.StudentRow(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.isActive(),
                        user.getCreatedAt(),
                        attempts.countByUserId(user.getId())
                ))
                .toList();
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        User user = users.findById(id)
                .orElseThrow(() -> new NotFound("Student not found"));

        if (user.getRole() != Role.STUDENT) {
            throw new NotFound("Student not found");
        }

        user.setActive(active);
        users.save(user);
    }
}
