package com.myenglishvocab.server.admin.service;

import com.myenglishvocab.server.admin.dto.*;
import com.myenglishvocab.server.analytics.entity.AccountLifecycleEvent;
import com.myenglishvocab.server.analytics.entity.AccountLifecycleType;
import com.myenglishvocab.server.analytics.entity.ActivityType;
import com.myenglishvocab.server.analytics.entity.UserActivityEvent;
import com.myenglishvocab.server.analytics.repository.AccountLifecycleEventRepository;
import com.myenglishvocab.server.analytics.repository.UserActivityEventRepository;
import com.myenglishvocab.server.common.exception.BusinessException;
import com.myenglishvocab.server.common.exception.ErrorCode;
import com.myenglishvocab.server.quiz.repository.QuizSetAttemptRepository;
import com.myenglishvocab.server.user.entity.UserStatus;
import com.myenglishvocab.server.user.repository.UserRepository;
import com.myenglishvocab.server.word.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatisticsService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final WordRepository wordRepository;
    private final QuizSetAttemptRepository quizSetAttemptRepository;
    private final UserActivityEventRepository activityEventRepository;
    private final AccountLifecycleEventRepository lifecycleEventRepository;

    public AdminOverviewResponse getOverview() {
        LocalDate today = LocalDate.now(KST);
        Instant todayStart = today.atStartOfDay(KST).toInstant();
        Instant tomorrowStart = today.plusDays(1).atStartOfDay(KST).toInstant();
        Instant sevenDaysStart = today.minusDays(6).atStartOfDay(KST).toInstant();
        Instant thirtyDaysStart = today.minusDays(29).atStartOfDay(KST).toInstant();

        long totalAccounts = userRepository.count();
        long activeAccounts = userRepository.countByStatus(UserStatus.ACTIVE);
        long withdrawnAccounts = userRepository.countByStatus(UserStatus.WITHDRAWN);
        long totalWords = wordRepository.count();
        double averageWords = activeAccounts == 0
                ? 0.0
                : Math.round((double) totalWords / activeAccounts * 100.0) / 100.0;

        return new AdminOverviewResponse(
                totalAccounts,
                activeAccounts,
                withdrawnAccounts,
                userRepository.countByCreatedAtIsNullAndStatus(UserStatus.ACTIVE),
                lifecycleEventRepository.countByEventTypeAndOccurredAtGreaterThanEqual(
                        AccountLifecycleType.SIGNUP,
                        sevenDaysStart
                ),
                wordRepository.countDistinctUsersWhoSavedSince(sevenDaysStart),
                totalWords,
                averageWords,
                quizSetAttemptRepository.countDistinctUsers(),
                activityEventRepository.findMostRecentActivityAt(),
                activityEventRepository.countDistinctActiveUsers(todayStart, tomorrowStart),
                activityEventRepository.countDistinctActiveUsers(thirtyDaysStart, tomorrowStart),
                activityEventRepository.countByEventType(ActivityType.PAGE_VIEW),
                activityEventRepository.countByEventTypeAndOccurredAtGreaterThanEqual(
                        ActivityType.PAGE_VIEW,
                        thirtyDaysStart
                ),
                activityEventRepository.countByEventType(ActivityType.AI_GENERATION_REQUESTED),
                activityEventRepository.countByEventTypeAndOccurredAtGreaterThanEqual(
                        ActivityType.AI_GENERATION_REQUESTED,
                        thirtyDaysStart
                ),
                lifecycleEventRepository.countByEventType(AccountLifecycleType.WITHDRAWAL)
        );
    }

    public List<DailyStatisticsResponse> getDailyStatistics(int days) {
        requireRange(days, 1, 365);

        LocalDate today = LocalDate.now(KST);
        LocalDate firstDate = today.minusDays(days - 1L);
        Instant from = firstDate.atStartOfDay(KST).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(KST).toInstant();

        Map<LocalDate, DailyAccumulator> values = new LinkedHashMap<>();
        for (int offset = 0; offset < days; offset++) {
            values.put(firstDate.plusDays(offset), new DailyAccumulator());
        }

        for (UserActivityEvent event
                : activityEventRepository.findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(from, to)) {
            LocalDate date = event.getOccurredAt().atZone(KST).toLocalDate();
            DailyAccumulator accumulator = values.get(date);
            if (accumulator == null) {
                continue;
            }
            accumulator.addActivity(event);
        }

        for (AccountLifecycleEvent event
                : lifecycleEventRepository.findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(from, to)) {
            LocalDate date = event.getOccurredAt().atZone(KST).toLocalDate();
            DailyAccumulator accumulator = values.get(date);
            if (accumulator != null) {
                accumulator.addLifecycle(event.getEventType());
            }
        }

        return values.entrySet().stream()
                .map(entry -> entry.getValue().toResponse(entry.getKey()))
                .toList();
    }

    public List<MonthlyStatisticsResponse> getMonthlyStatistics(int months) {
        requireRange(months, 1, 24);

        YearMonth currentMonth = YearMonth.now(KST);
        YearMonth firstMonth = currentMonth.minusMonths(months - 1L);
        Instant from = firstMonth.atDay(1).atStartOfDay(KST).toInstant();
        Instant to = currentMonth.plusMonths(1).atDay(1).atStartOfDay(KST).toInstant();

        Map<YearMonth, MonthlyAccumulator> values = new LinkedHashMap<>();
        for (int offset = 0; offset < months; offset++) {
            values.put(firstMonth.plusMonths(offset), new MonthlyAccumulator());
        }

        for (UserActivityEvent event
                : activityEventRepository.findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(from, to)) {
            YearMonth month = YearMonth.from(event.getOccurredAt().atZone(KST));
            MonthlyAccumulator accumulator = values.get(month);
            if (accumulator != null) {
                accumulator.addActivity(event);
            }
        }

        for (AccountLifecycleEvent event
                : lifecycleEventRepository.findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(from, to)) {
            YearMonth month = YearMonth.from(event.getOccurredAt().atZone(KST));
            MonthlyAccumulator accumulator = values.get(month);
            if (accumulator != null) {
                accumulator.addLifecycle(event.getEventType());
            }
        }

        return values.entrySet().stream()
                .map(entry -> entry.getValue().toResponse(entry.getKey()))
                .toList();
    }

    public List<PopularWordResponse> getPopularWords(int limit) {
        requireRange(limit, 1, 100);
        return wordRepository.findPopularWords(PageRequest.of(0, limit));
    }

    public List<PopularPageResponse> getPopularPages(int days, int limit) {
        requireRange(days, 1, 365);
        requireRange(limit, 1, 100);
        Instant from = LocalDate.now(KST)
                .minusDays(days - 1L)
                .atStartOfDay(KST)
                .toInstant();
        return activityEventRepository.findPopularPagesSince(from, PageRequest.of(0, limit));
    }

    public List<AdminUserResponse> getUsers(int limit) {
        requireRange(limit, 1, 500);
        return userRepository.findAllByOrderByLastActiveAtDesc(PageRequest.of(0, limit)).stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    public List<AccountLifecycleResponse> getAccountLifecycle(int limit) {
        requireRange(limit, 1, 500);
        return lifecycleEventRepository.findAllByOrderByOccurredAtDesc(PageRequest.of(0, limit)).stream()
                .map(AccountLifecycleResponse::from)
                .toList();
    }

    private void requireRange(int value, int min, int max) {
        if (value < min || value > max) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private static class DailyAccumulator {
        private final Set<Long> activeUserIds = new HashSet<>();
        private long newSignups;
        private long pageViews;
        private long aiGenerations;
        private long withdrawals;

        void addActivity(UserActivityEvent event) {
            if (event.getUser() != null) {
                activeUserIds.add(event.getUser().getId());
            }
            if (event.getEventType() == ActivityType.PAGE_VIEW) {
                pageViews++;
            } else if (event.getEventType() == ActivityType.AI_GENERATION_REQUESTED) {
                aiGenerations++;
            }
        }

        void addLifecycle(AccountLifecycleType type) {
            switch (type) {
                case SIGNUP -> newSignups++;
                case WITHDRAWAL -> withdrawals++;
            }
        }

        DailyStatisticsResponse toResponse(LocalDate date) {
            return new DailyStatisticsResponse(
                    date,
                    newSignups,
                    activeUserIds.size(),
                    pageViews,
                    aiGenerations,
                    withdrawals
            );
        }
    }

    private static class MonthlyAccumulator {
        private final Set<Long> activeUserIds = new HashSet<>();
        private long newSignups;
        private long pageViews;
        private long aiGenerations;
        private long withdrawals;

        void addActivity(UserActivityEvent event) {
            if (event.getUser() != null) {
                activeUserIds.add(event.getUser().getId());
            }
            if (event.getEventType() == ActivityType.PAGE_VIEW) {
                pageViews++;
            } else if (event.getEventType() == ActivityType.AI_GENERATION_REQUESTED) {
                aiGenerations++;
            }
        }

        void addLifecycle(AccountLifecycleType type) {
            switch (type) {
                case SIGNUP -> newSignups++;
                case WITHDRAWAL -> withdrawals++;
            }
        }

        MonthlyStatisticsResponse toResponse(YearMonth month) {
            return new MonthlyStatisticsResponse(
                    month.toString(),
                    newSignups,
                    activeUserIds.size(),
                    pageViews,
                    aiGenerations,
                    withdrawals
            );
        }
    }
}
