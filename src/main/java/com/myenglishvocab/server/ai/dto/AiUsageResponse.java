package com.myenglishvocab.server.ai.dto;

public record AiUsageResponse(
        int dailyLimit,
        int used,
        int remaining
) {
    public static AiUsageResponse of(int dailyLimit, int rawUsed) {
        int used = Math.clamp(rawUsed, 0, dailyLimit);
        int remaining = Math.max(0, dailyLimit - used);
        return new AiUsageResponse(dailyLimit, used, remaining);
    }
}
