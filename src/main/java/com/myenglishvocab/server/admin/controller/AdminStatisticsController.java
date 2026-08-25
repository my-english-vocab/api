package com.myenglishvocab.server.admin.controller;

import com.myenglishvocab.server.admin.dto.*;
import com.myenglishvocab.server.admin.service.AdminStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin Statistics", description = "관리자 전용 서비스 운영 통계 API")
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;

    @Operation(summary = "운영 통계 요약")
    @GetMapping("/overview")
    public ResponseEntity<AdminOverviewResponse> getOverview() {
        return ResponseEntity.ok(adminStatisticsService.getOverview());
    }

    @Operation(summary = "일별 가입·활동·페이지·AI·탈퇴 통계")
    @GetMapping("/daily")
    public ResponseEntity<List<DailyStatisticsResponse>> getDaily(
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(adminStatisticsService.getDailyStatistics(days));
    }

    @Operation(summary = "월별 가입·활동·페이지·AI·탈퇴 통계")
    @GetMapping("/monthly")
    public ResponseEntity<List<MonthlyStatisticsResponse>> getMonthly(
            @RequestParam(defaultValue = "12") int months
    ) {
        return ResponseEntity.ok(adminStatisticsService.getMonthlyStatistics(months));
    }

    @Operation(summary = "많이 저장된 단어")
    @GetMapping("/popular-words")
    public ResponseEntity<List<PopularWordResponse>> getPopularWords(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(adminStatisticsService.getPopularWords(limit));
    }

    @Operation(summary = "방문이 많은 페이지")
    @GetMapping("/popular-pages")
    public ResponseEntity<List<PopularPageResponse>> getPopularPages(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(adminStatisticsService.getPopularPages(days, limit));
    }

    @Operation(summary = "사용자 가입·최근 로그인·최근 활동 목록")
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> getUsers(
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(adminStatisticsService.getUsers(limit));
    }

    @Operation(summary = "회원가입·탈퇴 이력")
    @GetMapping("/account-lifecycle")
    public ResponseEntity<List<AccountLifecycleResponse>> getAccountLifecycle(
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(adminStatisticsService.getAccountLifecycle(limit));
    }
}
