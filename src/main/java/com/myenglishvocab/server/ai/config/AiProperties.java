package com.myenglishvocab.server.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * openai | gemini
     */
    private String provider = "openai";

    /**
     * 계정당 하루 generate-example 호출 한도
     */
    private int dailyLimit = 10;

    private OpenAi openai = new OpenAi();
    private Gemini gemini = new Gemini();

    private Timeout timeout = new Timeout();

    @Getter
    @Setter
    public static class OpenAi {
        private String apiKey = "";
        private String model = "gpt-4o-mini";
    }

    @Getter
    @Setter
    public static class Gemini {
        private String apiKey = "";
        private String model = "gemini-2.5-flash";
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Timeout {
        private Duration connect = Duration.ofSeconds(5);
        private Duration response = Duration.ofSeconds(30);
    }
}
