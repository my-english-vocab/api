package com.myenglishvocab.server.quiz.controller;

import com.myenglishvocab.server.auth.jwt.JwtPrincipal;
import com.myenglishvocab.server.quiz.dto.CompleteQuizSetAttemptRequest;
import com.myenglishvocab.server.quiz.dto.QuizSetAttemptSummaryResponse;
import com.myenglishvocab.server.quiz.service.QuizSetAttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Quiz Sets", description = "20개 단위 퀴즈 세트 완료 기록 API")
@RestController
@RequestMapping("/api/quiz/sets")
@RequiredArgsConstructor
public class QuizSetAttemptController {

    private final QuizSetAttemptService quizSetAttemptService;

    @Operation(summary = "내 퀴즈 세트 완료 횟수")
    @GetMapping("/attempts")
    public ResponseEntity<List<QuizSetAttemptSummaryResponse>> getMySummaries(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        return ResponseEntity.ok(quizSetAttemptService.getMySummaries(principal.userId()));
    }

    @Operation(
            summary = "퀴즈 세트 완료 기록",
            description = "마지막 문제까지 완료한 세트만 기록합니다. attemptId가 같으면 중복 집계하지 않습니다."
    )
    @PostMapping("/{setNumber}/attempts")
    public ResponseEntity<List<QuizSetAttemptSummaryResponse>> complete(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable int setNumber,
            @Valid @RequestBody CompleteQuizSetAttemptRequest request
    ) {
        List<QuizSetAttemptSummaryResponse> summaries = quizSetAttemptService.complete(
                principal.userId(),
                setNumber,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(summaries);
    }
}
