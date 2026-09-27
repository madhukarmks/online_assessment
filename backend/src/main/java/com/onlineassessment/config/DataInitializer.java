package com.onlineassessment.config;

import com.onlineassessment.entity.Assessment;
import com.onlineassessment.entity.Difficulty;
import com.onlineassessment.entity.Option;
import com.onlineassessment.entity.Question;
import com.onlineassessment.entity.QuestionType;
import com.onlineassessment.entity.Role;
import com.onlineassessment.entity.User;
import com.onlineassessment.repository.AssessmentRepository;
import com.onlineassessment.repository.QuestionRepository;
import com.onlineassessment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository users;
    private final AssessmentRepository assessments;
    private final QuestionRepository questions;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-sample-data:false}")
    private boolean seedSampleData;

    @Value("${app.seed-admin:false}")
    private boolean seedAdmin;

    @Value("${app.admin.email:admin@onlineassessment.local}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Bean
    CommandLineRunner init() {
        return args -> {
            if (seedAdmin) {
                createAdmin();
            }
            if (seedSampleData) {
                createSampleAssessment();
            }
        };
    }

    private void createAdmin() {
        if (adminPassword == null || adminPassword.length() < 8) {
            throw new IllegalStateException("ADMIN_PASSWORD must be configured when SEED_ADMIN=true");
        }

        if (users.findByEmail(adminEmail.trim().toLowerCase()).isEmpty()) {
            User admin = new User();
            admin.setName("System Admin");
            admin.setEmail(adminEmail.trim().toLowerCase());
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            users.save(admin);
        }
    }

    private void createSampleAssessment() {
        if (assessments.findAll().stream()
                .anyMatch(a -> "Java Fundamentals".equalsIgnoreCase(a.getTitle()))) {
            return;
        }

        Assessment assessment = new Assessment();
        assessment.setTitle("Java Fundamentals");
        assessment.setDescription("Sample assessment covering OOP, collections, exceptions, multithreading and Java basics.");
        assessment.setDurationMinutes(30);
        assessment.setTotalMarks(8.0);
        assessment.setPassingMarks(4.0);
        assessment.setDefaultNegativeMarks(0.25);
        assessment.setAttemptsAllowed(2);
        assessment.setStartAt(LocalDateTime.now().minusMinutes(5));
        assessment.setEndAt(LocalDateTime.now().plusDays(30));
        assessment.setPublished(true);

        List<Question> seedQuestions = new ArrayList<>();
        seedQuestions.add(addQuestion(assessment, 1,
                "Which concept allows a subclass to provide a specific implementation of a parent method?",
                QuestionType.MCQ_SINGLE, Difficulty.EASY,
                "Polymorphism allows overridden behavior to be selected at runtime.",
                List.of("Encapsulation", "Inheritance", "Polymorphism", "Abstraction"), 2));
        seedQuestions.add(addQuestion(assessment, 2,
                "Which Java collection does not allow duplicate elements?",
                QuestionType.MCQ_SINGLE, Difficulty.EASY,
                "Set implementations do not allow duplicate elements.",
                List.of("List", "Set", "Queue", "Map"), 1));
        seedQuestions.add(addQuestion(assessment, 3,
                "Which keyword is used to inherit a class in Java?",
                QuestionType.MCQ_SINGLE, Difficulty.EASY,
                "A class uses extends to inherit another class.",
                List.of("implements", "inherits", "extends", "super"), 2));
        seedQuestions.add(addQuestion(assessment, 4,
                "Which construct is commonly used to handle an exception?",
                QuestionType.MCQ_SINGLE, Difficulty.EASY,
                "Exceptions are handled using try/catch/finally constructs.",
                List.of("if", "switch", "try-catch", "for"), 2));
        seedQuestions.add(addQuestion(assessment, 5,
                "Which keyword prevents a class from being inherited?",
                QuestionType.MCQ_SINGLE, Difficulty.MEDIUM,
                "A final class cannot be extended.",
                List.of("static", "private", "final", "const"), 2));
        seedQuestions.add(addQuestion(assessment, 6,
                "Which interface represents a task that can be executed by a thread?",
                QuestionType.MCQ_SINGLE, Difficulty.MEDIUM,
                "Runnable is a functional interface representing a runnable task.",
                List.of("Runnable", "Serializable", "Cloneable", "Comparable"), 0));
        seedQuestions.add(addQuestion(assessment, 7,
                "Which of the following are Java 8 functional interfaces?",
                QuestionType.MCQ_MULTIPLE, Difficulty.MEDIUM,
                "Predicate and Function are functional interfaces in java.util.function.",
                List.of("Predicate", "Function", "ArrayList", "HashMap"), 0, 1));
        seedQuestions.add(addQuestion(assessment, 8,
                "Java supports multithreading.",
                QuestionType.TRUE_FALSE, Difficulty.EASY,
                "Java provides Thread, Runnable and concurrency utilities.",
                List.of("True", "False"), 0));

        assessments.save(assessment);
        questions.saveAll(seedQuestions);
    }

    private Question addQuestion(
            Assessment assessment,
            int order,
            String text,
            QuestionType type,
            Difficulty difficulty,
            String explanation,
            List<String> optionTexts,
            int... correctIndexes) {

        Question question = new Question();
        question.setAssessment(assessment);
        question.setQuestionText(text);
        question.setType(type);
        question.setDifficulty(difficulty);
        question.setMarks(1.0);
        question.setNegativeMarks(0.25);
        question.setExplanation(explanation);
        question.setDisplayOrder(order);

        for (int i = 0; i < optionTexts.size(); i++) {
            Option option = new Option();
            option.setQuestion(question);
            option.setOptionText(optionTexts.get(i));
            option.setDisplayOrder(i);
            option.setCorrect(contains(correctIndexes, i));
            question.getOptions().add(option);
        }

        return question;
    }

    private boolean contains(int[] values, int target) {
        for (int value : values) {
            if (value == target) return true;
        }
        return false;
    }
}
