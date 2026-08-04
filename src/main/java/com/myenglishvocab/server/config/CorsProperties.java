package com.myenglishvocab.server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    /**
     * 허용할 프론트엔드 Origin 목록 (예: http://localhost:3000)
     */
    private List<String> allowedOrigins = new ArrayList<>();
}
