package com.myenglishvocab.server.ai.service;

import com.myenglishvocab.server.ai.ExampleGenerator;
import com.myenglishvocab.server.ai.dto.ExamplePair;
import com.myenglishvocab.server.ai.dto.GenerateExampleRequest;
import com.myenglishvocab.server.ai.quota.AiUsageLimiter;
import com.myenglishvocab.server.ai.translation.Translator;
import com.myenglishvocab.server.analytics.entity.ActivityType;
import com.myenglishvocab.server.analytics.service.ActivityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class ExampleGenerationServiceTest {

    @Mock ExampleGenerator exampleGenerator;
    @Mock Translator translator;
    @Mock AiUsageLimiter aiUsageLimiter;
    @Mock ActivityService activityService;
    @InjectMocks ExampleGenerationService exampleGenerationService;

    @Test
    void AI_한도를_차감한_요청은_장기_통계에도_기록한다() {
        given(exampleGenerator.generate("apple", "사과"))
                .willReturn(new ExamplePair("I ate an apple.", "나는 사과를 먹었다."));

        var response = exampleGenerationService.generate(
                1L,
                new GenerateExampleRequest("apple", "사과")
        );

        assertThat(response.definition()).isEqualTo("사과");

        InOrder inOrder = inOrder(aiUsageLimiter, activityService, exampleGenerator);
        inOrder.verify(aiUsageLimiter).consume(1L);
        inOrder.verify(activityService).record(1L, ActivityType.AI_GENERATION_REQUESTED);
        inOrder.verify(exampleGenerator).generate("apple", "사과");
    }
}
