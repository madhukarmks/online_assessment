package com.onlineassessment.coding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlineassessment.entity.Question;
import com.onlineassessment.entity.QuestionType;
import com.onlineassessment.exception.Exceptions.BadRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CodingService {

    private final ObjectMapper mapper;

    @Value("${app.jdoodle.client-id:}")
    private String clientId;

    @Value("${app.jdoodle.client-secret:}")
    private String clientSecret;

    public record RunRequest(
            Long questionId,
            Integer languageId,
            String sourceCode,
            String stdin
    ) {}

    public record RunResponse(
            String status,
            Integer statusId,
            String stdout,
            String stderr,
            String compileOutput,
            String message,
            Double timeSeconds,
            Integer memoryKb
    ) {}

    public record TestCase(
            String input,
            String expectedOutput
    ) {}

    public record Evaluation(
            boolean passed,
            int passedTests,
            int totalTests,
            List<RunResponse> results
    ) {}

    private RestClient client() {
        return RestClient.builder()
                .baseUrl("https://api.jdoodle.com/v1")
                .build();
    }

    private Map<String, String> getLanguageAndVersion(Integer languageId) {
        Map<String, String> map = new HashMap<>();
        switch (languageId) {
            case 54: // C++
            case 76:
            case 105:
                map.put("language", "cpp17");
                map.put("versionIndex", "0");
                break;
            case 62: // Java
            case 91:
                map.put("language", "java");
                map.put("versionIndex", "4");
                break;
            case 71: // Python 3
            case 92:
                map.put("language", "python3");
                map.put("versionIndex", "4");
                break;
            case 63: // JavaScript
            case 93:
                map.put("language", "nodejs");
                map.put("versionIndex", "4");
                break;
            case 50: // C
            case 75:
                map.put("language", "c");
                map.put("versionIndex", "5");
                break;
            default:
                map.put("language", "cpp17");
                map.put("versionIndex", "0");
                break;
        }
        return map;
    }

    /*
     * Run Code Execution (Single execution using Custom Input or Test Case input)
     */
    public RunResponse run(
        Question question,
        Integer languageId,
        String sourceCode,
        String stdin
) {
    validate(question, languageId, sourceCode);

    try {
        Map<String, String> langConfig = getLanguageAndVersion(languageId);

        String finalInput = stdin;

        // AGAR CUSTOM INPUT KHALI HAI, TOH DB SE PEHLA HIDDEN TESTCASE PICK KARO
        if (finalInput == null || finalInput.trim().isEmpty()) {
            List<TestCase> tests = parseTests(question.getTestCasesJson());
            if (!tests.isEmpty() && tests.get(0).input() != null) {
                finalInput = tests.get(0).input(); // DB se "5" extract kar lega
            } else {
                finalInput = "0";
            }
        }

        // Standard newline taaki C++ / Java cin hang na ho
        if (!finalInput.endsWith("\n")) {
            finalInput = finalInput + "\n";
        }

        // Debug Log - Apne Console/Terminal par verify karne ke liye
        System.out.println("--> JDoodle Executing with Input: '" + finalInput.trim() + "'");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("clientId", clientId);
        body.put("clientSecret", clientSecret);
        body.put("script", sourceCode);
        body.put("stdin", finalInput); // Final Input (5\n) JDoodle ko bheja
        body.put("language", langConfig.get("language"));
        body.put("versionIndex", langConfig.get("versionIndex"));

        JsonNode response = client()
                .post()
                .uri("/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new BadRequest("JDoodle API execution failed.");
        }

        int statusCode = response.path("statusCode").asInt(200);
        String output = response.path("output").asText("");
        String memory = response.path("memory").asText(null);
        String cpuTime = response.path("cpuTime").asText(null);

        if (output.contains("JDoodle - Timeout")) {
            return new RunResponse(
                    "Time Limit Exceeded",
                    5,
                    "",
                    "Time Limit Exceeded. Program waited for input.",
                    "",
                    "Time Limit Exceeded",
                    null,
                    null
            );
        }

        if (statusCode != 200) {
            return new RunResponse(
                    "Runtime Error",
                    11,
                    "",
                    output,
                    "",
                    output,
                    null,
                    null
            );
        }

        return new RunResponse(
                "Accepted",
                3,
                output,
                "",
                "",
                "",
                cpuTime != null ? Double.parseDouble(cpuTime) : null,
                memory != null ? Integer.parseInt(memory) : null
        );

    } catch (BadRequest e) {
        throw e;
    } catch (Exception e) {
        e.printStackTrace();
        throw new BadRequest("Compiler error: " + (e.getMessage() == null ? "Unknown error" : e.getMessage()));
    }
}

    /*
     * Submit Evaluation: Hidden Test Cases (`2 4` -> `6`, `4 6` -> `10`) auto-checked sequentially
     */
    public Evaluation evaluate(
            Question question,
            Integer languageId,
            String sourceCode
    ) {
        validate(question, languageId, sourceCode);

        // Fetch array of hidden test cases configured in database/admin portal
        List<TestCase> tests = parseTests(question.getTestCasesJson());

        if (tests.isEmpty()) {
            throw new BadRequest("No coding test cases configured.");
        }

        List<RunResponse> results = new ArrayList<>();
        int passed = 0;

        for (TestCase test : tests) {
            // Send each hidden test case input directly to compiler
            RunResponse result = run(question, languageId, sourceCode, test.input());
            results.add(result);

            if (result.statusId() != null && result.statusId() == 3) {
                String actual = normalize(result.stdout());
                String expected = normalize(test.expectedOutput());

                if (actual.equals(expected)) {
                    passed++;
                }
            }
        }

        return new Evaluation(
                passed == tests.size(),
                passed,
                tests.size(),
                results
        );
    }

    private List<TestCase> parseTests(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return mapper.readValue(json, new TypeReference<List<TestCase>>() {});
        } catch (Exception e) {
            throw new BadRequest("Invalid coding test case JSON.");
        }
    }

    private void validate(Question question, Integer languageId, String code) {
        if (question == null || question.getType() != QuestionType.CODING) {
            throw new BadRequest("Coding question not found.");
        }
        if (languageId == null) {
            throw new BadRequest("Please select a programming language.");
        }
        if (code == null || code.isBlank()) {
            throw new BadRequest("Source code cannot be empty.");
        }

        String allowed = question.getAllowedLanguageIds();
        if (allowed == null || allowed.isBlank()) {
            throw new BadRequest("No programming languages enabled.");
        }

        boolean allowedLanguage = Arrays.stream(allowed.split(","))
                .map(String::trim)
                .anyMatch(id -> id.equals(String.valueOf(languageId)));

        if (!allowedLanguage) {
            throw new BadRequest("Selected language is not enabled.");
        }
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.trim().replace("\r\n", "\n").replace("\r", "\n").replaceAll("[ \\t]+(?=\\n)", "");
    }
}