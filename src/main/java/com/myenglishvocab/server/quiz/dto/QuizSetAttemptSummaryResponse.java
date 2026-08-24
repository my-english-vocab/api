package com.myenglishvocab.server.quiz.dto;

import java.time.Instant;

public record QuizSetAttemptSummaryResponse(
        int setNumber,
        long completedCount,
        Instant lastCompletedAt
) {
}
