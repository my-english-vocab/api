package com.myenglishvocab.server.ai;

import com.myenglishvocab.server.ai.config.AiProperties;
import com.myenglishvocab.server.ai.dto.ExamplePair;
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
public class OpenAiExampleGenerator implements ExampleGenerator {

    private static final String URL = "https://api.openai.com/v1/chat/completions";

    private final AiProperties aiProperties;
    private final JsonMapper jsonMapper;
    private final RestClient.Builder restClientBuilder;

    @Override
    public ExamplePair generate(String term, String definition) {
        String apiKey = aiProperties.getOpenai().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        }

        String prompt = """
                You help Korean learners of English.
                Given term="%s" and definition="%s", create one English example sentence
                and its Korean meaning.

                Style requirements for exampleSentence:
                - Sound like everyday spoken American English (US conversational).
                - Prefer casual, natural phrasing people actually say in daily life in the US.
                - Avoid stiff textbook English, overly formal writing, or unnatural literal translations.
                - Keep it one clear sentence that naturally uses the term.

                Reply with ONLY valid JSON, no markdown, no code fence:
                {"exampleSentence":"...","meaningOfExampleSentence":"..."}
                """.formatted(term, definition);

        try {
            Map<String, Object> body = Map.of(
                    "model", aiProperties.getOpenai().getModel(),
                    "temperature", 0.4,
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
                        log.warn("OpenAI 예문 HTTP 오류 status={} body={}", response.getStatusCode(), errorBody);
                        throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
                    })
                    .body(String.class);

            JsonNode root = jsonMapper.readTree(raw);
            String text = root.path("choices").path(0).path("message").path("content").asString();
            if (!StringUtils.hasText(text)) {
                log.warn("OpenAI 예문 응답 비어 있음 raw={}", raw);
                throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
            }
            return parseResponse(text.trim());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("OpenAI 예문 생성 실패 term={}", term, e);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
    }

    private ExamplePair parseResponse(String text) {
        String json = text;
        if (json.startsWith("```")) {
            json = json.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
        }

        JsonNode node = jsonMapper.readTree(json);
        String example = node.path("exampleSentence").asString();
        String meaning = node.path("meaningOfExampleSentence").asString();

        if (!StringUtils.hasText(example) || !StringUtils.hasText(meaning)) {
            log.warn("OpenAI JSON 파싱 실패 text={}", text);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED);
        }
        return new ExamplePair(example, meaning);
    }
}
