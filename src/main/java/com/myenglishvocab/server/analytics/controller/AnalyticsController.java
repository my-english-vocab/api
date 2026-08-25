package com.myenglishvocab.server.analytics.controller;

import com.myenglishvocab.server.analytics.dto.PageViewRequest;
import com.myenglishvocab.server.analytics.entity.ActivityType;
import com.myenglishvocab.server.analytics.service.ActivityService;
import com.myenglishvocab.server.auth.jwt.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analytics", description = "인증 사용자 활동 기록 API")
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ActivityService activityService;

    @Operation(summary = "페이지 방문 기록", description = "쿼리 문자열을 제외한 화면 경로만 기록합니다.")
    @PostMapping("/page-views")
    public ResponseEntity<Void> recordPageView(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody PageViewRequest request
    ) {
        activityService.record(principal.userId(), ActivityType.PAGE_VIEW, request.path());
        return ResponseEntity.noContent().build();
    }
}
