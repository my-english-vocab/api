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
                                
                                ## 인증 API
                                - POST /api/auth/signup — 회원가입
                                - POST /api/auth/login — body에 accessToken, Set-Cookie(httpOnly)에 refreshToken
                                - POST /api/auth/refresh — 쿠키의 refresh token으로 access/refresh token 교체(RTR)
                                - POST /api/auth/logout — Refresh 쿠키/Redis 무효화
                                - GET /api/auth/me — 현재 로그인 사용자 조회 (Bearer 인증 필요)

                                ## 인증 방법
                                1. POST /api/auth/login 응답의 accessToken 값을 복사합니다.
                                2. Swagger의 Authorize를 누르고 bearerAuth 입력칸에 accessToken만 붙여넣습니다.
                                3. Swagger가 Authorization 헤더에 Bearer 접두어를 자동으로 추가합니다.
                                refresh와 logout은 httpOnly 쿠키를 사용합니다.

                                ## 단어장 API (모두 Bearer 인증 필요)
                                - GET /api/words — 내 단어 목록
                                - GET /api/words/{id} — 내 단어 단건 조회
                                - POST /api/words — 단어 추가
                                - PUT /api/words/{id} — 단어 수정
                                - DELETE /api/words/{id} — 단어 삭제
                                - POST /api/words/{id}/mark-learned — 외웠음 (level + 1)
                                - POST /api/words/generate-example — AI 뜻/예문 생성 (저장하지 않음)

                                ## AI API (Bearer 인증 필요)
                                - GET /api/ai/usage — 오늘 AI 사용량과 남은 횟수 조회

                                ## 에러 응답
                                공통 형식: { code, message, timestamp, path }
                                - USER_DUPLICATE_USERNAME
                                - USER_NOT_FOUND
                                - AUTH_INVALID_CREDENTIALS
                                - AUTH_INVALID_REFRESH_TOKEN
                                - WORD_NOT_FOUND
                                - COMMON_INVALID_INPUT
                                - COMMON_INTERNAL_ERROR
                                - AI_GENERATION_FAILED
                                - AI_NOT_CONFIGURED
                                - AI_DAILY_LIMIT_EXCEEDED
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
