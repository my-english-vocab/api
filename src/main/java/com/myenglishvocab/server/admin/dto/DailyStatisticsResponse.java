package com.myenglishvocab.server.admin.dto;

import java.time.LocalDate;

public record DailyStatisticsResponse(
        LocalDate date,
        long newSignups,
        long activeUsers,
        long pageViews,
        long aiGenerationRequests,
        long withdrawals
) {
}
