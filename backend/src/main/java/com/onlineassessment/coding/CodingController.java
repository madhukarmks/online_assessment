package com.onlineassessment.coding;

import com.onlineassessment.dto.ApiResponse;
import com.onlineassessment.entity.Question;
import com.onlineassessment.exception.Exceptions.NotFound;
import com.onlineassessment.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coding")
@RequiredArgsConstructor
public class CodingController {
    private final CodingService coding;
    private final QuestionRepository questions;

    @GetMapping("/languages")
    public ApiResponse<?> languages() {
        return ApiResponse.ok("Languages", List.of(
                Map.of("id",50,"name","C (GCC)"), Map.of("id",54,"name","C++ (GCC)"),
                Map.of("id",62,"name","Java"), Map.of("id",71,"name","Python 3"),
                Map.of("id",63,"name","JavaScript (Node.js)")
        ));
    }

    @PostMapping("/run")
    public ApiResponse<?> run(@RequestBody CodingService.RunRequest request) {
        Question q = questions.findById(request.questionId()).orElseThrow(() -> new NotFound("Question not found"));
        return ApiResponse.ok("Code executed", coding.run(q, request.languageId(), request.sourceCode(), request.stdin()));
    }
}
