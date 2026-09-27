package com.onlineassessment.service;

import com.onlineassessment.dto.AttemptDtos.AnswerRequest;
import com.onlineassessment.dto.AttemptDtos.AttemptQuestion;
import com.onlineassessment.dto.AttemptDtos.AttemptView;
import com.onlineassessment.dto.AttemptDtos.StartResponse;
import com.onlineassessment.coding.CodingService;
import com.onlineassessment.dto.QuestionDtos.PublicOption;
import com.onlineassessment.dto.ResultDtos;
import com.onlineassessment.entity.Answer;
import com.onlineassessment.entity.Assessment;
import com.onlineassessment.entity.Attempt;
import com.onlineassessment.entity.AttemptStatus;
import com.onlineassessment.entity.Option;
import com.onlineassessment.entity.Question;
import com.onlineassessment.entity.QuestionType;
import com.onlineassessment.entity.Result;
import com.onlineassessment.entity.User;
import com.onlineassessment.exception.Exceptions.BadRequest;
import com.onlineassessment.exception.Exceptions.Forbidden;
import com.onlineassessment.exception.Exceptions.NotFound;
import com.onlineassessment.repository.AnswerRepository;
import com.onlineassessment.repository.AssessmentRepository;
import com.onlineassessment.repository.AttemptRepository;
import com.onlineassessment.repository.QuestionRepository;
import com.onlineassessment.repository.ResultRepository;
import com.onlineassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttemptService {

    private final AttemptRepository attempts;
    private final AnswerRepository answers;
    private final ResultRepository results;
    private final QuestionRepository questions;
    private final AssessmentRepository assessments;
    private final UserRepository users;
    private final MappingService mapper;
    private final CodingService coding;

    @Transactional
    public StartResponse start(Long assessmentId, String email) {
        User user = user(email);
        Assessment assessment = assessment(assessmentId);

        if (!assessment.isPublished()) {
            throw new BadRequest("Assessment is not published");
        }

        LocalDateTime now = LocalDateTime.now();

        if (assessment.getStartAt() != null && now.isBefore(assessment.getStartAt())) {
            throw new BadRequest("Assessment is not available yet");
        }

        if (assessment.getEndAt() != null && !now.isBefore(assessment.getEndAt())) {
            throw new BadRequest("Assessment has expired");
        }

        if (questions.countByAssessmentId(assessmentId) == 0) {
            throw new BadRequest("Assessment has no questions");
        }

        // Refreshing/re-opening an active attempt resumes it instead of creating another attempt.
        Attempt existing = attempts.findByUserIdOrderByStartedAtDesc(user.getId())
                .stream()
                .filter(a -> a.getAssessment().getId().equals(assessmentId))
                .filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS)
                .findFirst()
                .orElse(null);

        if (existing != null) {
            if (!LocalDateTime.now().isBefore(existing.getEndsAt())) {
                submitInternal(existing, true);
            } else {
                return startResponse(existing, assessment, now);
            }
        }

        long previousAttempts = attempts.countByUserIdAndAssessmentId(user.getId(), assessmentId);

        if (previousAttempts >= assessment.getAttemptsAllowed()) {
            throw new BadRequest("Attempt limit reached");
        }

        Attempt attempt = new Attempt();
        attempt.setUser(user);
        attempt.setAssessment(assessment);
        attempt.setStartedAt(now);
        attempt.setEndsAt(calculateEndTime(now, assessment));
        attempt.setStatus(AttemptStatus.IN_PROGRESS);

        attempts.save(attempt);

        return startResponse(attempt, assessment, now);
    }

    private StartResponse startResponse(Attempt attempt, Assessment assessment, LocalDateTime serverNow) {
        return new StartResponse(
                attempt.getId(),
                assessment.getId(),
                assessment.getTitle(),
                serverNow,
                attempt.getEndsAt(),
                questions.findByAssessmentIdOrderByDisplayOrderAscIdAsc(assessment.getId())
                        .stream()
                        .map(mapper::pub)
                        .toList()
        );
    }

    private LocalDateTime calculateEndTime(LocalDateTime startedAt, Assessment assessment) {
        LocalDateTime durationEnd = startedAt.plusMinutes(assessment.getDurationMinutes());
        if (assessment.getEndAt() == null) {
            return durationEnd;
        }
        return durationEnd.isBefore(assessment.getEndAt())
                ? durationEnd
                : assessment.getEndAt();
    }

    @Transactional
    public AttemptView view(Long attemptId, String email) {
        Attempt attempt = owned(attemptId, email);

        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS
                && !LocalDateTime.now().isBefore(attempt.getEndsAt())) {
            submitInternal(attempt, true);
        }

        List<Answer> storedAnswers = answers.findByAttemptId(attemptId);
        Map<Long, Answer> answerMap = new HashMap<>();
        storedAnswers.forEach(answer -> answerMap.put(answer.getQuestion().getId(), answer));

        List<AttemptQuestion> questionViews =
                questions.findByAssessmentIdOrderByDisplayOrderAscIdAsc(attempt.getAssessment().getId())
                        .stream()
                        .map(question -> {
                            Answer answer = answerMap.get(question.getId());
                            Set<Long> selectedOptions = answer == null || answer.getSelectedOptionIds() == null
                                    ? Set.of()
                                    : Set.copyOf(answer.getSelectedOptionIds());
                            boolean markedForReview = answer != null && answer.isMarkedForReview();

                            List<PublicOption> options = question.getOptions()
                                    .stream()
                                    .sorted(java.util.Comparator.comparing(Option::getDisplayOrder))
                                    .map(option -> new PublicOption(
                                            option.getId(),
                                            option.getOptionText(),
                                            option.getDisplayOrder()
                                    ))
                                    .toList();

                            return new AttemptQuestion(
                                    question.getId(),
                                    question.getQuestionText(),
                                    question.getType(),
                                    options,
                                    selectedOptions,
                                    markedForReview,
                                    question.getMarks(),
                                    question.getStarterCode(),
                                    question.getAllowedLanguageIds()==null?List.of():java.util.Arrays.stream(question.getAllowedLanguageIds().split(",")).filter(x->!x.isBlank()).map(Integer::valueOf).toList(),
                                    answer==null?null:answer.getCode(),
                                    answer==null?null:answer.getCodeLanguageId()
                            );
                        })
                        .toList();

        return new AttemptView(
                attempt.getId(),
                attempt.getAssessment().getId(),
                attempt.getAssessment().getTitle(),
                attempt.getStartedAt(),
                attempt.getEndsAt(),
                attempt.getStatus().name(),
                questionViews
        );
    }

    @Transactional
    public void saveAnswer(Long attemptId, AnswerRequest request, String email) {
        Attempt attempt = owned(attemptId, email);
        ensureActive(attempt);

        Question question = questions.findById(request.questionId())
                .orElseThrow(() -> new NotFound("Question not found"));

        if (!question.getAssessment().getId().equals(attempt.getAssessment().getId())) {
            throw new BadRequest("Question does not belong to assessment");
        }

        Set<Long> selectedOptionIds = request.selectedOptionIds() == null
                ? Set.of()
                : request.selectedOptionIds();

        Set<Long> validOptionIds = question.getOptions()
                .stream()
                .map(Option::getId)
                .collect(Collectors.toSet());

        if (question.getType() != QuestionType.CODING && !validOptionIds.containsAll(selectedOptionIds)) {
            throw new BadRequest("Invalid option");
        }

        if (question.getType() != QuestionType.CODING && (question.getType() == QuestionType.MCQ_SINGLE
                || question.getType() == QuestionType.TRUE_FALSE)
                && selectedOptionIds.size() > 1) {
            throw new BadRequest("This question allows only one option");
        }

        Answer answer = answers.findByAttemptIdAndQuestionId(attemptId, question.getId())
                .orElseGet(() -> {
                    Answer newAnswer = new Answer();
                    newAnswer.setAttempt(attempt);
                    newAnswer.setQuestion(question);
                    return newAnswer;
                });

        answer.setSelectedOptionIds(new HashSet<>(selectedOptionIds));
        answer.setCode(request.code());
        answer.setCodeLanguageId(request.codeLanguageId());
        answer.setMarkedForReview(request.markedForReview());
        answers.save(answer);
    }

    @Transactional
    public ResultDtos.Detail submit(Long attemptId, String email) {
        Attempt attempt = owned(attemptId, email);

        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            boolean automatic = !LocalDateTime.now().isBefore(attempt.getEndsAt());
            return submitInternal(attempt, automatic);
        }

        return detailForResult(findResultByAttemptId(attemptId));
    }

    @Transactional
    protected ResultDtos.Detail submitInternal(Attempt attempt, boolean automatic) {
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return detailForResult(findResultByAttemptId(attempt.getId()));
        }

        LocalDateTime submittedAt = LocalDateTime.now();
        attempt.setStatus(automatic ? AttemptStatus.AUTO_SUBMITTED : AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(submittedAt);
        attempts.save(attempt);

        List<Question> questionList = questions.findByAssessmentIdOrderByDisplayOrderAscIdAsc(
                attempt.getAssessment().getId()
        );

        Map<Long, Answer> answerMap = new HashMap<>();
        answers.findByAttemptId(attempt.getId())
                .forEach(answer -> answerMap.put(answer.getQuestion().getId(), answer));

        int attempted = 0;
        int correct = 0;
        int wrong = 0;
        double obtained = 0.0;
        double negative = 0.0;
        List<ResultDtos.QuestionAnalysis> analysis = new ArrayList<>();

        for (Question question : questionList) {
            Answer answer = answerMap.get(question.getId());
            Set<Long> selected = answer == null || answer.getSelectedOptionIds() == null
                    ? Set.of()
                    : Set.copyOf(answer.getSelectedOptionIds());
            String status = "UNANSWERED";
            double marks = 0.0;

            if (question.getType() == QuestionType.CODING && answer != null && answer.getCode() != null && !answer.getCode().isBlank()) {
                CodingService.Evaluation evaluation = coding.evaluate(question, answer.getCodeLanguageId(), answer.getCode());
                attempted++;
                if (evaluation.passed()) { correct++; obtained += question.getMarks(); status = "CORRECT"; marks = question.getMarks(); }
                else { wrong++; double neg=getNegativeMarks(question, attempt.getAssessment()); negative += neg; obtained -= neg; marks=-neg; status="WRONG"; }
                analysis.add(new ResultDtos.QuestionAnalysis(question.getId(), question.getQuestionText(), selected, Set.of(), status, marks));
                continue;
            }

            Set<Long> correctIds = question.getOptions()
                    .stream()
                    .filter(Option::isCorrect)
                    .map(Option::getId)
                    .collect(Collectors.toSet());

            if (!selected.isEmpty()) {
                attempted++;

                if (selected.equals(correctIds)) {
                    correct++;
                    marks = question.getMarks();
                    obtained += marks;
                    status = "CORRECT";
                } else {
                    wrong++;
                    double neg = getNegativeMarks(question, attempt.getAssessment());
                    negative += neg;
                    obtained -= neg;
                    marks = -neg;
                    status = "WRONG";
                }
            }

            analysis.add(new ResultDtos.QuestionAnalysis(
                    question.getId(),
                    question.getQuestionText(),
                    selected,
                    correctIds,
                    status,
                    marks
            ));
        }

        double totalMarks = attempt.getAssessment().getTotalMarks();
        double percentage = totalMarks <= 0.0 ? 0.0 : (obtained / totalMarks) * 100.0;

        Result result = new Result();
        result.setAttempt(attempt);
        result.setUser(attempt.getUser());
        result.setAssessment(attempt.getAssessment());
        result.setTotalQuestions(questionList.size());
        result.setAttemptedQuestions(attempted);
        result.setUnattemptedQuestions(questionList.size() - attempted);
        result.setCorrectAnswers(correct);
        result.setWrongAnswers(wrong);
        result.setTotalMarks(totalMarks);
        result.setObtainedMarks(obtained);
        result.setNegativeMarks(negative);
        result.setPercentage(percentage);
        result.setPassed(obtained >= attempt.getAssessment().getPassingMarks());
        result.setTimeTakenSeconds(Math.max(0L, Duration.between(
                attempt.getStartedAt(), submittedAt
        ).getSeconds()));
        result.setSubmittedAt(submittedAt);

        results.save(result);

        return new ResultDtos.Detail(new ResultDtos.Summary(
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
        ), analysis);
    }

    private ResultDtos.Detail detailForResult(Result result) {
        List<Answer> storedAnswers = answers.findByAttemptId(result.getAttempt().getId());
        Map<Long, Answer> answerMap = new HashMap<>();
        storedAnswers.forEach(answer -> answerMap.put(answer.getQuestion().getId(), answer));

        List<ResultDtos.QuestionAnalysis> analysis =
                questions.findByAssessmentIdOrderByDisplayOrderAscIdAsc(result.getAssessment().getId())
                        .stream()
                        .map(question -> {
                            Answer answer = answerMap.get(question.getId());
                            Set<Long> selected = answer == null || answer.getSelectedOptionIds() == null
                                    ? Set.of()
                                    : Set.copyOf(answer.getSelectedOptionIds());
                            Set<Long> correct = question.getOptions()
                                    .stream()
                                    .filter(Option::isCorrect)
                                    .map(Option::getId)
                                    .collect(Collectors.toSet());

                            boolean isCorrect = !selected.isEmpty() && selected.equals(correct);
                            String status = selected.isEmpty() ? "UNANSWERED" : isCorrect ? "CORRECT" : "WRONG";
                            double marks = isCorrect
                                    ? question.getMarks()
                                    : selected.isEmpty()
                                    ? 0.0
                                    : -getNegativeMarks(question, result.getAssessment());

                            return new ResultDtos.QuestionAnalysis(
                                    question.getId(),
                                    question.getQuestionText(),
                                    selected,
                                    correct,
                                    status,
                                    marks
                            );
                        })
                        .toList();

        return new ResultDtos.Detail(summary(result), analysis);
    }

    private ResultDtos.Summary summary(Result result) {
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

    private double getNegativeMarks(Question question, Assessment assessment) {
        if (question.getNegativeMarks() != null) {
            return question.getNegativeMarks();
        }
        return assessment.getDefaultNegativeMarks() == null
                ? 0.0
                : assessment.getDefaultNegativeMarks();
    }

    private Result findResultByAttemptId(Long attemptId) {
        return results.findByAttemptId(attemptId)
                .orElseThrow(() -> new NotFound("Result not found"));
    }

    private Attempt owned(Long attemptId, String email) {
        User user = user(email);
        return attempts.findByIdAndUserId(attemptId, user.getId())
                .orElseThrow(() -> new Forbidden("Attempt not accessible"));
    }

    private void ensureActive(Attempt attempt) {
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequest("Attempt already submitted");
        }

        if (!LocalDateTime.now().isBefore(attempt.getEndsAt())) {
            submitInternal(attempt, true);
            throw new BadRequest("Assessment time expired");
        }
    }

    private User user(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new NotFound("User not found"));
    }

    private Assessment assessment(Long id) {
        return assessments.findById(id)
                .orElseThrow(() -> new NotFound("Assessment not found"));
    }
}
