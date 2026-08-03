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
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai", matchIfMissing = true)
@RequiredArgsConstructor
public class OpenAiTranslator implements Translator {

    private static final String URL = "https://api.openai.com/v1/chat/completions";

    private final AiProperties aiProperties;
    private final JsonMapper jsonMapper;
    private final RestClient.Builder restClientBuilder;

    @Override
    public String translateToKorean(String englishText) {
        String apiKey = aiProperties.getOpenai().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        }

        String prompt = """
                Translate the English word or short phrase into Korean.
                Reply with ONLY the most common short Korean meaning.
                Use one word or a very short phrase. No explanation. No quotes.

                Input: %s
                """.formatted(englishText);

        String content = chat(apiKey, prompt);
        String korean = content.replace("\"", "").trim();
        if (!StringUtils.hasText(korean)) {
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
        return korean;
    }

    private String chat(String apiKey, String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", aiProperties.getOpenai().getModel(),
                    "temperature", 0.2,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            String raw = restClientBuilder.build()
                    .post()
                    .uri(URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.value() >= 400, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        log.warn("OpenAI 번역 HTTP 오류 status={} body={}", response.getStatusCode(), errorBody);
                        throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
                    })
                    .body(String.class);

            JsonNode root = jsonMapper.readTree(raw);
            String text = root.path("choices").path(0).path("message").path("content").asString();
            if (!StringUtils.hasText(text)) {
                log.warn("OpenAI 번역 응답 비어 있음 raw={}", raw);
                throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
            }
            return text.trim();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("OpenAI 번역 실패", e);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
    }
}
