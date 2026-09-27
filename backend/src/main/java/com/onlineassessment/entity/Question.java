package com.onlineassessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "questions",
        indexes = @Index(name = "idx_question_assessment", columnList = "assessment_id")
)
@Getter
@Setter
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty = Difficulty.MEDIUM;

    @Column(nullable = false)
    private Double marks = 1.0;

    @Column(nullable = false)
    private Double negativeMarks = 0.0;

    @Column(length = 2000)
    private String explanation;

    @Column(columnDefinition = "LONGTEXT")
    private String starterCode;

    @Column(length = 500)
    private String allowedLanguageIds;

    @Column(columnDefinition = "LONGTEXT")
    private String testCasesJson;

    @Column(nullable = false)
    private Integer displayOrder = 0;

    @OneToMany(
            mappedBy = "question",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Option> options = new ArrayList<>();

    private LocalDateTime createdAt;

    @PrePersist
    void createTimestamp() {
        createdAt = LocalDateTime.now();
    }
}
