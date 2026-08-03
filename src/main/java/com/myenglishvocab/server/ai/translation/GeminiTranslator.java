package com.myenglishvocab.server.ai.translation;

import com.myenglishvocab.server.ai.config.AiProperties;
import com.myenglishvocab.server.common.exception.BusinessException;
import com.myenglishvocab.server.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini")
@RequiredArgsConstructor
public class GeminiTranslator implements Translator {

    private final AiProperties aiProperties;
    private final JsonMapper jsonMapper;
    private final RestClient.Builder restClientBuilder;

    @Override
    public String translateToKorean(String englishText) {
        var gemini = aiProperties.getGemini();
        if (!gemini.isEnabled() || !StringUtils.hasText(gemini.getApiKey())) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        }

        String prompt = """
                Translate the English word or short phrase into Korean.
                Reply with ONLY the most common short Korean meaning.
                Use one word or a very short phrase. No explanation. No quotes.

                Input: %s
                """.formatted(englishText);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent"
                .formatted(gemini.getModel());

        try {
            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    )
            );

            String raw = restClientBuilder.build()
                    .post()
                    .uri(url)
                    .header("x-goog-api-key", gemini.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.value() >= 400, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        log.warn("Gemini 번역 HTTP 오류 status={} body={}", response.getStatusCode(), errorBody);
                        throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
                    })
                    .body(String.class);

            String korean = extractText(raw).replace("\"", "").trim();
            if (!StringUtils.hasText(korean)) {
                throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
            }
            return korean;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Gemini 번역 실패 text={}", englishText, e);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
    }

    private String extractText(String raw) {
        JsonNode root = jsonMapper.readTree(raw);
        String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asString();
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
        return text.trim();
    }
}
