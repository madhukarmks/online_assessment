package com.onlineassessment.repository;

import com.onlineassessment.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResultRepository extends JpaRepository<Result, Long> {
    List<Result> findByUserIdOrderBySubmittedAtDesc(Long id);
    Optional<Result> findByIdAndUserId(Long id, Long uid);
    Optional<Result> findByAttemptId(Long attemptId);
    long countByAssessmentId(Long assessmentId);
    List<Result> findByAssessmentIdOrderByObtainedMarksDescSubmittedAtAscIdAsc(Long id);
}
