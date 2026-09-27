package com.onlineassessment.controller;

import com.onlineassessment.dto.ApiResponse;
import com.onlineassessment.dto.AttemptDtos.AnswerRequest;
import com.onlineassessment.service.AssessmentService;
import com.onlineassessment.service.AttemptService;
import com.onlineassessment.service.ResultService;
import com.onlineassessment.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final AssessmentService assessments;
    private final AttemptService attempts;
    private final ResultService results;
    private final StudentService student;

    @GetMapping("/dashboard")
    public ApiResponse<?> dashboard(Authentication authentication) {
        return ApiResponse.ok("Student dashboard", student.dashboard(authentication.getName()));
    }

    @GetMapping("/assessments")
    public ApiResponse<?> assessments() {
        return ApiResponse.ok("Available assessments", assessments.published());
    }

    @GetMapping("/assessments/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        return ApiResponse.ok("Assessment", assessments.detail(id));
    }

    @PostMapping("/assessments/{id}/start")
    public ApiResponse<?> start(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok("Attempt started", attempts.start(id, authentication.getName()));
    }

    @GetMapping("/attempts/history")
    public ApiResponse<?> history(Authentication authentication) {
        return ApiResponse.ok("Attempt history", student.history(authentication.getName()));
    }

    @GetMapping("/attempts/{id}")
    public ApiResponse<?> attempt(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok("Attempt", attempts.view(id, authentication.getName()));
    }

    @PostMapping("/attempts/{id}/answers")
    public ApiResponse<?> answer(
            @PathVariable Long id,
            @Valid @RequestBody AnswerRequest request,
            Authentication authentication) {
        attempts.saveAnswer(id, request, authentication.getName());
        return ApiResponse.ok("Answer saved");
    }

    @PostMapping("/attempts/{id}/submit")
    public ApiResponse<?> submit(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok(
                "Assessment submitted",
                attempts.submit(id, authentication.getName())
        );
    }

    @GetMapping("/results")
    public ApiResponse<?> results(Authentication authentication) {
        return ApiResponse.ok("Results", results.mine(authentication.getName()));
    }

    @GetMapping("/results/{id}")
    public ApiResponse<?> result(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok("Result", results.get(id, authentication.getName()));
    }
}
