package com.onlineassessment.service;

import com.onlineassessment.dto.ResultDtos;
import com.onlineassessment.entity.Answer;
import com.onlineassessment.entity.Option;
import com.onlineassessment.entity.Question;
import com.onlineassessment.entity.Result;
import com.onlineassessment.entity.User;
import com.onlineassessment.exception.Exceptions.Forbidden;
import com.onlineassessment.exception.Exceptions.NotFound;
import com.onlineassessment.repository.AnswerRepository;
import com.onlineassessment.repository.QuestionRepository;
import com.onlineassessment.repository.ResultRepository;
import com.onlineassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResultService {

    private final ResultRepository results;
    private final UserRepository users;
    private final AnswerRepository answers;
    private final QuestionRepository questions;

    @Transactional(readOnly = true)
    public List<ResultDtos.Summary> mine(String email) {
        User user = user(email);
        return results.findByUserIdOrderBySubmittedAtDesc(user.getId())
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResultDtos.Summary get(Long id, String email) {
        User user = user(email);
        Result result = results.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new Forbidden("Result not accessible"));
        return map(result);
    }

    @Transactional(readOnly = true)
    public List<ResultDtos.Summary> leaderboard(Long assessmentId) {
        return results.findByAssessmentIdOrderByObtainedMarksDescSubmittedAtAscIdAsc(assessmentId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResultDtos.Summary> all(Long assessmentId, Long studentId) {
        return results.findAll()
                .stream()
                .filter(r -> assessmentId == null || r.getAssessment().getId().equals(assessmentId))
                .filter(r -> studentId == null || r.getUser().getId().equals(studentId))
                .sorted((a, b) -> {
                    if (a.getSubmittedAt() == null && b.getSubmittedAt() == null) return 0;
                    if (a.getSubmittedAt() == null) return 1;
                    if (b.getSubmittedAt() == null) return -1;
                    return b.getSubmittedAt().compareTo(a.getSubmittedAt());
                })
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResultDtos.Detail adminDetail(Long id) {
        Result result = results.findById(id)
                .orElseThrow(() -> new NotFound("Result not found"));
        return detail(result);
    }

    private ResultDtos.Detail detail(Result result) {
        List<Answer> storedAnswers = answers.findByAttemptId(result.getAttempt().getId());
        Map<Long, Answer> answerMap = new HashMap<>();
        storedAnswers.forEach(a -> answerMap.put(a.getQuestion().getId(), a));

        List<ResultDtos.QuestionAnalysis> analysis =
                questions.findByAssessmentIdOrderByDisplayOrderAscIdAsc(result.getAssessment().getId())
                        .stream()
                        .map(question -> analysis(question, answerMap.get(question.getId())))
                        .toList();

        return new ResultDtos.Detail(map(result), analysis);
    }

    private ResultDtos.QuestionAnalysis analysis(Question question, Answer answer) {
        Set<Long> selected = answer == null || answer.getSelectedOptionIds() == null
                ? Set.of()
                : Set.copyOf(answer.getSelectedOptionIds());

        Set<Long> correct = question.getOptions().stream()
                .filter(Option::isCorrect)
                .map(Option::getId)
                .collect(Collectors.toSet());

        boolean isCorrect = !selected.isEmpty() && selected.equals(correct);
        String status = selected.isEmpty() ? "UNANSWERED" : isCorrect ? "CORRECT" : "WRONG";
        double marks = isCorrect ? question.getMarks() : selected.isEmpty() ? 0.0 : -question.getNegativeMarks();

        return new ResultDtos.QuestionAnalysis(
                question.getId(),
                question.getQuestionText(),
                selected,
                correct,
                status,
                marks
        );
    }

    private ResultDtos.Summary map(Result result) {
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

    private User user(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new NotFound("User not found"));
    }
}
