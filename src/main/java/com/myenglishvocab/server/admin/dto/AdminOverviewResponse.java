package com.myenglishvocab.server.admin.dto;

import java.time.Instant;

public record AdminOverviewResponse(
        long totalAccounts,
        long activeAccounts,
        long withdrawnAccounts,
        long legacyAccountsWithoutSignupDate,
        long newSignupsLast7Days,
        long wordUsersLast7Days,
        long totalSavedWords,
        double averageWordsPerActiveAccount,
        long quizUsers,
        Instant mostRecentActivityAt,
        long dailyActiveUsers,
        long monthlyActiveUsers,
        long totalPageViews,
        long pageViewsLast30Days,
        long totalAiGenerationRequests,
        long aiGenerationRequestsLast30Days,
        long totalWithdrawals
) {
}
