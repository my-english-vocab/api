package com.myenglishvocab.server.quiz.entity;

import com.myenglishvocab.server.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "quiz_set_attempts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_quiz_set_attempts_user_attempt",
                columnNames = {"user_id", "attempt_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizSetAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private UUID attemptId;

    @Column(nullable = false, updatable = false)
    private int setNumber;

    @Column(nullable = false, updatable = false)
    private int wordCount;

    @Column(nullable = false, updatable = false)
    private int learnedCount;

    @Column(nullable = false, updatable = false)
    private Instant completedAt;

    public QuizSetAttempt(
            User user,
            UUID attemptId,
            int setNumber,
            int wordCount,
            int learnedCount
    ) {
        this.user = user;
        this.attemptId = attemptId;
        this.setNumber = setNumber;
        this.wordCount = wordCount;
        this.learnedCount = learnedCount;
    }

    @PrePersist
    void onCreate() {
        this.completedAt = Instant.now();
    }
}
