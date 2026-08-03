package com.myenglishvocab.server.ai.controller;

import com.myenglishvocab.server.ai.config.AiProperties;
import com.myenglishvocab.server.ai.dto.AiUsageResponse;
import com.myenglishvocab.server.ai.quota.AiUsageLimiter;
import com.myenglishvocab.server.auth.jwt.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI", description = "AI 사용량 조회 API")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiUsageLimiter aiUsageLimiter;
    private final AiProperties aiProperties;

    @Operation(summary = "오늘 AI 사용량 조회", description = "계정당 하루 한도 기준 used/remaining을 반환합니다.")
    @GetMapping("/usage")
    public ResponseEntity<AiUsageResponse> getUsage(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        int used = aiUsageLimiter.getUsedCount(principal.userId());
        return ResponseEntity.ok(AiUsageResponse.of(aiProperties.getDailyLimit(), used));
    }
}