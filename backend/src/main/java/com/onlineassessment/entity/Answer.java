package com.onlineassessment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "answers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attempt_question",
                columnNames = {"attempt_id", "question_id"}
        ),
        indexes = @Index(name = "idx_answer_attempt", columnList = "attempt_id")
)
@Getter
@Setter
@NoArgsConstructor
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private Attempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ElementCollection
    @CollectionTable(
            name = "answer_selected_options",
            joinColumns = @JoinColumn(name = "answer_id")
    )
    @Column(name = "option_id", nullable = false)
    private Set<Long> selectedOptionIds = new HashSet<>();

    @Column(columnDefinition = "LONGTEXT")
    private String code;

    private Integer codeLanguageId;

    @Column(nullable = false)
    private boolean markedForReview = false;
}
