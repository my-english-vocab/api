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
                                2. POST /api/auth/login — body에 accessToken, Set-Cookie(httpOnly)에 refreshToken
                                3. Authorize에 Bearer {accessToken} 입력 후 보호 API 호출
                                4. Access 만료 시 POST /api/auth/refresh (쿠키의 refresh 사용, rotation)
                                5. POST /api/auth/logout — Refresh 쿠키/Redis 무효화
                                
                                ## 단어장 API (Bearer 인증 필요)
                                - GET /api/words — 내 단어 목록
                                - POST /api/words — 단어 추가
                                - PUT /api/words/{id} — 단어 수정
                                - DELETE /api/words/{id} — 단어 삭제
                                - POST /api/words/{id}/mark-learned — 외웠음 (level + 1)
                                
                                ## 에러 응답
                                공통 형식: { code, message, timestamp, path }
                                - USER_DUPLICATE_USERNAME
                                - USER_NOT_FOUND
                                - AUTH_INVALID_CREDENTIALS
                                - AUTH_INVALID_REFRESH_TOKEN
                                - WORD_NOT_FOUND
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