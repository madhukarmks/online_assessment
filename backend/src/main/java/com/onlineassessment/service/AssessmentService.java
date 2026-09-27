package com.onlineassessment.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onlineassessment.dto.AssessmentDtos;
import com.onlineassessment.dto.AssessmentDtos.Detail;
import com.onlineassessment.dto.AssessmentDtos.Summary;
import com.onlineassessment.dto.QuestionDtos;
import com.onlineassessment.dto.QuestionDtos.AdminQuestion;
import com.onlineassessment.dto.QuestionDtos.OptionRequest;
import com.onlineassessment.entity.Assessment;
import com.onlineassessment.entity.Option;
import com.onlineassessment.entity.Question;
import com.onlineassessment.entity.QuestionType;
import com.onlineassessment.exception.Exceptions.BadRequest;
import com.onlineassessment.exception.Exceptions.NotFound;
import com.onlineassessment.repository.AnswerRepository;
import com.onlineassessment.repository.AssessmentRepository;
import com.onlineassessment.repository.AttemptRepository;
import com.onlineassessment.repository.QuestionRepository;
import com.onlineassessment.repository.ResultRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository assessments;
    private final QuestionRepository questions;
    private final AttemptRepository attempts;
    private final AnswerRepository answers;
    private final ResultRepository results;
    private final MappingService mapper;

    private Summary summary(Assessment assessment) {
        return new Summary(
                assessment.getId(),
                assessment.getTitle(),
                assessment.getDescription(),
                assessment.getDurationMinutes(),
                assessment.getTotalMarks(),
                assessment.getPassingMarks(),
                assessment.getStartAt(),
                assessment.getEndAt(),
                assessment.getAttemptsAllowed(),
                assessment.isPublished(),
                (int) questions.countByAssessmentId(assessment.getId())
        );
    }

    @Transactional(readOnly = true)
    public List<Summary> all() {
        return assessments.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::summary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Summary> published() {
        return assessments.findByPublishedTrueOrderByStartAtAsc()
                .stream()
                .map(this::summary)
                .toList();
    }

    public Summary create(AssessmentDtos.Request request) {
        return summary(save(new Assessment(), request));
    }

    public Summary update(Long id, AssessmentDtos.Request request) {
        Assessment assessment = get(id);
        return summary(save(assessment, request));
    }

    private Assessment save(
            Assessment assessment,
            AssessmentDtos.Request request
    ) {
        if (request.passingMarks() > request.totalMarks()) {
            throw new BadRequest("Passing marks cannot exceed total marks");
        }
        if (request.startAt() != null && request.endAt() != null
                && !request.startAt().isBefore(request.endAt())) {
            throw new BadRequest("Start time must be before end time");
        }

        assessment.setTitle(request.title().trim());
        assessment.setDescription(request.description());
        assessment.setDurationMinutes(request.durationMinutes());
        assessment.setTotalMarks(request.totalMarks());
        assessment.setPassingMarks(request.passingMarks());
        assessment.setDefaultNegativeMarks(request.defaultNegativeMarks());
        assessment.setStartAt(request.startAt());
        assessment.setEndAt(request.endAt());
        assessment.setAttemptsAllowed(request.attemptsAllowed());
        assessment.setPublished(request.published());

        return assessments.save(assessment);
    }

    public void delete(Long id) {

        get(id);

        if (attempts.countByAssessmentId(id) > 0 || results.countByAssessmentId(id) > 0) {
            throw new BadRequest("Assessment cannot be deleted after attempts have been created");
        }

        if (questions.countByAssessmentId(id) > 0) {
            throw new BadRequest("Delete questions before deleting assessment");
        }

        assessments.deleteById(id);
    }
       @Transactional(readOnly=true)
    public Detail detail(Long id) {

        Assessment assessment = get(id);

        return new Detail(
                summary(assessment),
                questions.findByAssessmentIdOrderByDisplayOrderAscIdAsc(id)
                        .stream()
                        .map(mapper::pub)
                        .toList()
        );
    }

    public Assessment get(Long id) {
        return assessments.findById(id)
                .orElseThrow(
                        () -> new NotFound("Assessment not found")
                );
    }

    @Transactional
    public AdminQuestion addQuestion(
            Long assessmentId,
            QuestionDtos.Request request
    ) {

        Assessment assessment = get(assessmentId);

        validateOptions(request);

        Question question = new Question();

        question.setAssessment(assessment);

        apply(question, request);

        questions.save(question);

        return mapper.admin(question);
    }

    @Transactional
    public AdminQuestion updateQuestion(
            Long questionId,
            QuestionDtos.Request request
    ) {

        Question question = questions.findById(questionId)
                .orElseThrow(
                        () -> new NotFound("Question not found")
                );

        validateOptions(request);

        question.setQuestionText(request.questionText());
        question.setType(request.type());
        question.setDifficulty(request.difficulty());
        question.setMarks(request.marks());
        question.setNegativeMarks(request.negativeMarks());
        question.setExplanation(request.explanation());
        question.setStarterCode(request.starterCode());
        question.setAllowedLanguageIds(request.allowedLanguageIds()==null?null:request.allowedLanguageIds().stream().distinct().map(String::valueOf).reduce((a,b)->a+","+b).orElse(null));
        question.setTestCasesJson(request.testCasesJson());

        question.setDisplayOrder(
                request.displayOrder() == null
                        ? 0
                        : request.displayOrder()
        );

        /*
         * Remove old options.
         */
        question.getOptions().clear();

        /*
         * Add updated options.
         */
        if (request.type() == QuestionType.CODING) return mapper.admin(question);

        for (OptionRequest optionRequest : request.options()) {

            Option option = new Option();

            option.setQuestion(question);
            option.setOptionText(optionRequest.optionText());
            option.setCorrect(optionRequest.correct());

            option.setDisplayOrder(
                    optionRequest.displayOrder() == null
                            ? 0
                            : optionRequest.displayOrder()
            );

            question.getOptions().add(option);
        }

        return mapper.admin(question);
    }

    private void apply(
            Question question,
            QuestionDtos.Request request
    ) {

        question.setQuestionText(request.questionText());
        question.setType(request.type());
        question.setDifficulty(request.difficulty());
        question.setMarks(request.marks());
        question.setNegativeMarks(request.negativeMarks());
        question.setExplanation(request.explanation());
        question.setStarterCode(request.starterCode());
        question.setAllowedLanguageIds(request.allowedLanguageIds()==null?null:request.allowedLanguageIds().stream().distinct().map(String::valueOf).reduce((a,b)->a+","+b).orElse(null));
        question.setTestCasesJson(request.testCasesJson());

        question.setDisplayOrder(
                request.displayOrder() == null
                        ? 0
                        : request.displayOrder()
        );

        if (request.type() == QuestionType.CODING) return;

        for (OptionRequest optionRequest : request.options()) {

            Option option = new Option();

            option.setQuestion(question);
            option.setOptionText(optionRequest.optionText());
            option.setCorrect(optionRequest.correct());

            option.setDisplayOrder(
                    optionRequest.displayOrder() == null
                            ? 0
                            : optionRequest.displayOrder()
            );

            question.getOptions().add(option);
        }
    }

    private void validateOptions(
            QuestionDtos.Request request
    ) {

        if (request.type() == QuestionType.CODING) {
            if (request.allowedLanguageIds() == null || request.allowedLanguageIds().isEmpty()) throw new BadRequest("Select at least one coding language");
            if (request.testCasesJson() == null || request.testCasesJson().isBlank()) throw new BadRequest("Add at least one coding test case");
            return;
        }

        if (request.options() == null || request.options().size() < 2) throw new BadRequest("At least two options are required");

        long correctCount = request.options()
                .stream()
                .filter(OptionRequest::correct)
                .count();

        if (request.type() == QuestionType.MCQ_SINGLE
                && correctCount != 1) {

            throw new BadRequest(
                    "Single-choice question needs exactly one correct option"
            );
        }

        if (request.type() == QuestionType.MCQ_MULTIPLE
                && correctCount < 1) {

            throw new BadRequest(
                    "Multiple-choice question needs at least one correct option"
            );
        }

        if (request.type() == QuestionType.TRUE_FALSE
                && (request.options().size() != 2
                || correctCount != 1)) {

            throw new BadRequest(
                    "True/False requires exactly two options and one correct option"
            );
        }
    }

    @Transactional(readOnly = true)
    public List<AdminQuestion> adminQuestions(Long assessmentId) {

        get(assessmentId);

        return questions
                .findByAssessmentIdOrderByDisplayOrderAscIdAsc(assessmentId)
                .stream()
                .map(mapper::admin)
                .toList();
    }

    public void deleteQuestion(Long questionId) {

        Question question = questions.findById(questionId)
                .orElseThrow(() -> new NotFound("Question not found"));

        if (answers.countByQuestionId(questionId) > 0) {
            throw new BadRequest("Question cannot be deleted after students have answered it");
        }

        questions.delete(question);
    }
}