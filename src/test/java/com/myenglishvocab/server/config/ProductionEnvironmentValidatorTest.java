package com.myenglishvocab.server.config;

import com.myenglishvocab.server.ai.config.AiProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionEnvironmentValidatorTest {

    @Test
    void JWT_secret이_32자보다_짧으면_검증에_실패한다() {
        JwtProperties jwtProperties = validJwtProperties();
        jwtProperties.setSecret("too-short-secret");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                new ProductionEnvironmentValidator(jwtProperties, openAiProperties()).validate()
        );

        assertTrue(exception.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void OpenAI_provider인데_API_key가_없으면_검증에_실패한다() {
        AiProperties aiProperties = openAiProperties();
        aiProperties.getOpenai().setApiKey("");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                new ProductionEnvironmentValidator(validJwtProperties(), aiProperties).validate()
        );

        assertTrue(exception.getMessage().contains("OPENAI_API_KEY"));
    }

    @Test
    void Gemini_provider인데_API_key가_없으면_검증에_실패한다() {
        AiProperties aiProperties = geminiProperties();
        aiProperties.getGemini().setApiKey("");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                new ProductionEnvironmentValidator(validJwtProperties(), aiProperties).validate()
        );

        assertTrue(exception.getMessage().contains("GEMINI_API_KEY"));
    }

    @Test
    void Gemini_provider인데_기능이_비활성화되어_있으면_검증에_실패한다() {
        AiProperties aiProperties = geminiProperties();
        aiProperties.getGemini().setEnabled(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                new ProductionEnvironmentValidator(validJwtProperties(), aiProperties).validate()
        );

        assertTrue(exception.getMessage().contains("GEMINI_ENABLED"));
    }

    @Test
    void Gemini_provider인데_model이_비어_있으면_검증에_실패한다() {
        AiProperties aiProperties = geminiProperties();
        aiProperties.getGemini().setModel(" ");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                new ProductionEnvironmentValidator(validJwtProperties(), aiProperties).validate()
        );

        assertTrue(exception.getMessage().contains("ai.gemini.model"));
    }

    @Test
    void 지원하지_않는_AI_provider면_검증에_실패한다() {
        AiProperties aiProperties = openAiProperties();
        aiProperties.setProvider("unknown");

        assertThrows(IllegalStateException.class, () ->
                new ProductionEnvironmentValidator(validJwtProperties(), aiProperties).validate()
        );
    }

    @Test
    void 정상적인_운영_설정은_검증을_통과한다() {
        assertDoesNotThrow(() ->
                new ProductionEnvironmentValidator(validJwtProperties(), openAiProperties()).validate()
        );
    }

    @Test
    void 정상적인_Gemini_운영_설정은_검증을_통과한다() {
        assertDoesNotThrow(() ->
                new ProductionEnvironmentValidator(validJwtProperties(), geminiProperties()).validate()
        );
    }

    private JwtProperties validJwtProperties() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-must-be-at-least-32-characters-long");
        return jwtProperties;
    }

    private AiProperties openAiProperties() {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setProvider("openai");
        aiProperties.getOpenai().setApiKey("test-openai-api-key");
        return aiProperties;
    }

    private AiProperties geminiProperties() {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setProvider("gemini");
        aiProperties.getGemini().setEnabled(true);
        aiProperties.getGemini().setApiKey("test-gemini-api-key");
        aiProperties.getGemini().setModel("gemini-2.5-flash");
        return aiProperties;
    }
}
