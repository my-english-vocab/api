package com.myenglishvocab.server.ai.service;

import com.myenglishvocab.server.analytics.entity.ActivityType;
import com.myenglishvocab.server.analytics.service.ActivityService;
import com.myenglishvocab.server.ai.ExampleGenerator;
import com.myenglishvocab.server.ai.dto.ExamplePair;
import com.myenglishvocab.server.ai.dto.GenerateExampleRequest;
import com.myenglishvocab.server.ai.dto.GenerateExampleResponse;
import com.myenglishvocab.server.ai.quota.AiUsageLimiter;
import com.myenglishvocab.server.ai.translation.Translator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExampleGenerationService {

    private final ExampleGenerator exampleGenerator;
    private final Translator translator;
    private final AiUsageLimiter aiUsageLimiter;
    private final ActivityService activityService;

    public GenerateExampleResponse generate(Long userId, GenerateExampleRequest request) {
        aiUsageLimiter.consume(userId);
        activityService.record(userId, ActivityType.AI_GENERATION_REQUESTED);

        String term = request.term().trim();
        String definition = resolveDefinition(term, request.definition());

        log.info("예문 생성 userId={} term={} definition={}", userId, term, definition);

        ExamplePair pair = exampleGenerator.generate(term, definition);

        return new GenerateExampleResponse(
                definition,
                pair.exampleSentence(),
                pair.meaningOfExampleSentence()
        );
    }

    private String resolveDefinition(String term, String given) {
        if (StringUtils.hasText(given)) {
            return given.trim();
        }
        String korean = translator.translateToKorean(term);
        if (korean.length() > 150) {
            korean = korean.substring(0, 150);
        }
        return korean;
    }
}
