package com.myenglishvocab.server.word.controller;

import com.myenglishvocab.server.ai.dto.GenerateExampleRequest;
import com.myenglishvocab.server.ai.dto.GenerateExampleResponse;
import com.myenglishvocab.server.ai.service.ExampleGenerationService;
import com.myenglishvocab.server.auth.jwt.JwtPrincipal;
import com.myenglishvocab.server.common.exception.ErrorResponse;
import com.myenglishvocab.server.word.dto.CreateWordRequest;
import com.myenglishvocab.server.word.dto.UpdateFavoriteRequest;
import com.myenglishvocab.server.word.dto.UpdateWordRequest;
import com.myenglishvocab.server.word.dto.WordResponse;
import com.myenglishvocab.server.word.service.WordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Words", description = "내 단어장 CRUD, 즐겨찾기 및 학습(레벨) API")
@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;
    private final ExampleGenerationService exampleGenerationService;

    @Operation(summary = "내 단어 목록", description = "저장 순서(createdAt 오름차순)로 반환합니다.")
    @GetMapping
    public ResponseEntity<List<WordResponse>> getMyWords(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        return ResponseEntity.ok(wordService.getMyWords(principal.userId()));
    }

    @Operation(summary = "단어 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "단어 없음 또는 소유자 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<WordResponse> getMyWord(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(wordService.getMyWord(principal.userId(), id));
    }

    @Operation(summary = "단어 추가")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<WordResponse> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateWordRequest request
    ) {
        WordResponse response = wordService.create(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "단어 수정", description = "term/definition/예문만 수정합니다. level은 변경되지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "단어 없음 또는 소유자 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<WordResponse> update(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateWordRequest request
    ) {
        return ResponseEntity.ok(wordService.update(principal.userId(), id, request));
    }

    @Operation(summary = "단어 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "단어 없음 또는 소유자 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long id
    ) {
        wordService.delete(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "외웠음 처리",
            description = "퀴즈에서 '외웠어요' 선택 시 호출합니다. 서버에서 level을 1 증가시킵니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "레벨 증가 성공"),
            @ApiResponse(responseCode = "404", description = "단어 없음 또는 소유자 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/mark-learned")
    public ResponseEntity<WordResponse> markLearned(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(wordService.markLearned(principal.userId(), id));
    }

    @Operation(
            summary = "즐겨찾기 상태 변경",
            description = "favorite 값으로 내 단어의 즐겨찾기 상태를 설정합니다. 같은 값을 다시 요청해도 결과는 동일합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "즐겨찾기 상태 변경 성공"),
            @ApiResponse(responseCode = "400", description = "favorite 누락 또는 입력값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "단어 없음 또는 소유자 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/favorite")
    public ResponseEntity<WordResponse> updateFavorite(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateFavoriteRequest request
    ) {
        return ResponseEntity.ok(wordService.updateFavorite(principal.userId(), id, request));
    }

    @Operation(
            summary = "예문 AI 생성",
            description = """
                term은 필수, definition은 생략 가능합니다.
                definition이 없으면 영어 단어를 짧은 한국어 뜻으로 채운 뒤 예문/해석을 생성합니다.
                계정당 하루 10회까지 사용 가능합니다(한국 시간 기준).
                결과는 저장되지 않으며, 확인 후 POST /api/words 로 저장하세요.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "AI 생성 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "AI 미설정",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "하루 사용량 초과",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/generate-example")
    public ResponseEntity<GenerateExampleResponse> generateExample(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody GenerateExampleRequest request
    ) {
        return ResponseEntity.ok(exampleGenerationService.generate(principal.userId(), request));
    }
}
