package com.myenglishvocab.server.quiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CompleteQuizSetAttemptRequest(
        @NotNull UUID attemptId,
        @Min(1) @Max(30) int wordCount,
        @Min(0) @Max(30) int learnedCount
) {
}
