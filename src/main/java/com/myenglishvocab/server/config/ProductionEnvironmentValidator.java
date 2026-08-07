package com.myenglishvocab.server.config;

import com.myenglishvocab.server.ai.config.AiProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProductionEnvironmentValidator {

    private final JwtProperties jwtProperties;
    private final AiProperties aiProperties;

    @PostConstruct
    void validate() {
        validateJwtSecret();
        validateAiConfiguration();
    }

    private void validateJwtSecret() {
        String secret = jwtProperties.getSecret();

        if (!StringUtils.hasText(secret) || secret.length() < 32) {
            throw new IllegalStateException(
                    "운영 환경에서는 JWT_SECRET이 32자 이상이어야 합니다."
            );
        }
    }

    private void validateAiConfiguration() {
        String provider = aiProperties.getProvider();

        if ("openai".equals(provider)) {
            validateRequiredValue(
                    "OPENAI_API_KEY",
                    aiProperties.getOpenai().getApiKey()
            );
            return;
        }

        if ("gemini".equals(provider)) {
            validateGeminiConfiguration();
            return;
        }

        throw new IllegalStateException(
                "ai.provider는 openai 또는 gemini여야 합니다."
        );
    }

    private void validateGeminiConfiguration() {
        AiProperties.Gemini gemini = aiProperties.getGemini();

        if (!gemini.isEnabled()) {
            throw new IllegalStateException(
                    "ai.provider가 gemini이면 GEMINI_ENABLED는 true여야 합니다."
            );
        }

        validateRequiredValue("GEMINI_API_KEY", gemini.getApiKey());
        validateRequiredValue("ai.gemini.model", gemini.getModel());
    }

    private void validateRequiredValue(String name, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(
                    "운영 환경에서는 " + name + "가 필요합니다."
            );
        }
    }
}
