package com.onlineassessment.controller;

import com.onlineassessment.dto.ApiResponse;
import com.onlineassessment.dto.AssessmentDtos;
import com.onlineassessment.dto.QuestionDtos;
import com.onlineassessment.service.AdminService;
import com.onlineassessment.service.AssessmentService;
import com.onlineassessment.service.ResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService admin;
    private final AssessmentService assessment;
    private final ResultService results;

    @GetMapping("/dashboard")
    public ApiResponse<?> dashboard() {
        return ApiResponse.ok("Dashboard", admin.dashboard());
    }

    @GetMapping("/students")
    public ApiResponse<?> students(@RequestParam(required = false) String search) {
        return ApiResponse.ok("Students", admin.students(search));
    }

    @PutMapping("/students/{id}/active")
    public ApiResponse<?> active(@PathVariable Long id, @RequestParam boolean value) {
        admin.setActive(id, value);
        return ApiResponse.ok("Student status updated");
    }

    @GetMapping("/assessments")
    public ApiResponse<?> allAssessments() {
        return ApiResponse.ok("Assessments", assessment.all());
    }

    @PostMapping("/assessments")
    public ApiResponse<?> createAssessment(@Valid @RequestBody AssessmentDtos.Request request) {
        return ApiResponse.ok("Assessment created", assessment.create(request));
    }

    @GetMapping("/assessments/{id}")
    public ApiResponse<?> assessmentDetail(@PathVariable Long id) {
        return ApiResponse.ok("Assessment", assessment.detail(id));
    }

    @PutMapping("/assessments/{id}")
    public ApiResponse<?> updateAssessment(
            @PathVariable Long id,
            @Valid @RequestBody AssessmentDtos.Request request) {
        return ApiResponse.ok("Assessment updated", assessment.update(id, request));
    }

    @DeleteMapping("/assessments/{id}")
    public ApiResponse<?> deleteAssessment(@PathVariable Long id) {
        assessment.delete(id);
        return ApiResponse.ok("Assessment deleted");
    }

    @GetMapping("/assessments/{id}/questions")
    public ApiResponse<?> questions(@PathVariable Long id) {
        return ApiResponse.ok("Questions", assessment.adminQuestions(id));
    }

    @PostMapping("/assessments/{id}/questions")
    public ApiResponse<?> addQuestion(
            @PathVariable Long id,
            @Valid @RequestBody QuestionDtos.Request request) {
        return ApiResponse.ok("Question created", assessment.addQuestion(id, request));
    }

    @PutMapping("/questions/{id}")
    public ApiResponse<?> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody QuestionDtos.Request request) {
        return ApiResponse.ok("Question updated", assessment.updateQuestion(id, request));
    }

    @DeleteMapping("/questions/{id}")
    public ApiResponse<?> deleteQuestion(@PathVariable Long id) {
        assessment.deleteQuestion(id);
        return ApiResponse.ok("Question deleted");
    }

    @GetMapping("/results")
    public ApiResponse<?> results(
            @RequestParam(required = false) Long assessmentId,
            @RequestParam(required = false) Long studentId) {
        return ApiResponse.ok("Results", results.all(assessmentId, studentId));
    }

    @GetMapping("/results/{id}")
    public ApiResponse<?> result(@PathVariable Long id) {
        return ApiResponse.ok("Result", results.adminDetail(id));
    }
}
