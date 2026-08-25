package com.myenglishvocab.server.quiz.repository;

import com.myenglishvocab.server.quiz.dto.QuizSetAttemptSummaryResponse;
import com.myenglishvocab.server.quiz.entity.QuizSetAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizSetAttemptRepository extends JpaRepository<QuizSetAttempt, Long> {

    Optional<QuizSetAttempt> findByUserIdAndAttemptId(Long userId, UUID attemptId);

    void deleteByUserId(Long userId);

    @Query("SELECT COUNT(DISTINCT attempt.user.id) FROM QuizSetAttempt attempt")
    long countDistinctUsers();

    @Query("""
            SELECT new com.myenglishvocab.server.quiz.dto.QuizSetAttemptSummaryResponse(
                attempt.setNumber,
                COUNT(attempt),
                MAX(attempt.completedAt)
            )
            FROM QuizSetAttempt attempt
            WHERE attempt.user.id = :userId
            GROUP BY attempt.setNumber
            ORDER BY attempt.setNumber
            """)
    List<QuizSetAttemptSummaryResponse> findSummariesByUserId(Long userId);
}
