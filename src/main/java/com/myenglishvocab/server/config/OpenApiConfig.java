package com.myenglishvocab.server.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MyEnglishVocab API")
                        .description("""
                                영어 단어장 백엔드 API입니다.
                                
                                ## 인증 흐름
                                1. POST /api/auth/signup — 회원가입
                                2. POST /api/auth/login — accessToken 발급
                                3. Authorize에 Bearer {token} 입력
                                4. GET /api/auth/me 등 보호 API 호출
                                
                                ## 에러 응답
                                공통 형식: { code, message, timestamp, path }
                                - USER_DUPLICATE_USERNAME
                                - AUTH_INVALID_CREDENTIALS
                                - COMMON_INVALID_INPUT
                                - COMMON_INTERNAL_ERROR
                                """)
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}