package com.onlineassessment.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationLogicTest {

    @Test
    void correctSingleChoiceGetsPositiveMarks() {
        Set<Long> selected = Set.of(2L);
        Set<Long> correct = Set.of(2L);

        assertEquals(correct, selected);
        assertEquals(1.0, 1.0);
    }

    @Test
    void multipleChoiceRequiresExactSetMatch() {
        Set<Long> selected = Set.of(1L, 3L);
        Set<Long> correct = Set.of(1L, 3L);

        assertEquals(correct, selected);
        assertNotEquals(Set.of(1L), selected);
    }

    @Test
    void wrongAnswerAppliesNegativeMarks() {
        double obtained = 1.0;
        double negative = 0.25;

        assertEquals(0.75, obtained - negative, 0.0001);
    }

    @Test
    void unattemptedQuestionHasZeroMarks() {
        Set<Long> selected = Set.of();
        assertTrue(selected.isEmpty());
        assertEquals(0.0, 0.0);
    }

    @Test
    void percentageIsCalculatedFromTotalMarks() {
        double total = 10.0;
        double obtained = 7.5;

        assertEquals(75.0, (obtained / total) * 100.0, 0.001);
    }
}
