package com.onlineassessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "results",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_result_attempt",
                columnNames = "attempt_id"
        ),
        indexes = @Index(
                name = "idx_result_assessment_score",
                columnList = "assessment_id,obtained_marks"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private Attempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(nullable = false)
    private int totalQuestions;

    @Column(nullable = false)
    private int attemptedQuestions;

    @Column(nullable = false)
    private int unattemptedQuestions;

    @Column(nullable = false)
    private int correctAnswers;

    @Column(nullable = false)
    private int wrongAnswers;

    @Column(nullable = false)
    private double totalMarks;

    @Column(nullable = false)
    private double obtainedMarks;

    @Column(nullable = false)
    private double negativeMarks;

    @Column(nullable = false)
    private double percentage;

    @Column(nullable = false)
    private boolean passed;

    @Column(nullable = false)
    private long timeTakenSeconds;

    private LocalDateTime submittedAt;
}
