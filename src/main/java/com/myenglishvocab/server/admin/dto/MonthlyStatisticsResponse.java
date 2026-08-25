package com.myenglishvocab.server.admin.dto;

public record MonthlyStatisticsResponse(
        String month,
        long newSignups,
        long activeUsers,
        long pageViews,
        long aiGenerationRequests,
        long withdrawals
) {
}
