# MyEnglishVocab Server

Spring Boot 기반 영어 단어장 API 서버입니다.  
JWT Access Token 인증과 표준 에러 응답을 적용했습니다.

## Tech Stack
- Java 21, Spring Boot 4
- Spring Security + JWT (jjwt)
- Spring Data JPA, H2
- SpringDoc OpenAPI

## 로컬 실행

### 1) 환경변수
```bash
cp .env.example .env
```

`.env`에서 `JWT_SECRET`을 32자 이상으로 설정하세요.  
또는 `application-local.yaml`(gitignore)을 사용해도 됩니다.

```bash
export JWT_SECRET=local-dev-secret-key-must-be-at-least-32-characters-long
export SPRING_PROFILES_ACTIVE=local
```

### 2) 서버 실행
```bash
./gradlew bootRun
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- H2 Console: http://localhost:8080/h2-console  
  - JDBC URL: `jdbc:h2:file:./vocabdb`
  - Username: `sa`
  - Password: (비움)

### 3) 테스트
```bash
./gradlew test
```

## 인증 흐름
1. `POST /api/auth/signup` — 회원가입
2. `POST /api/auth/login` — `accessToken` 발급
3. `Authorization: Bearer {accessToken}` 헤더 추가
4. `GET /api/auth/me` — 인증된 사용자 정보 조회

Swagger UI에서 **Authorize** 버튼으로 Bearer 토큰을 등록한 뒤 보호 API를 호출할 수 있습니다.

## 에러 응답 형식
```json
{
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "아이디 또는 비밀번호가 올바르지 않습니다.",
  "timestamp": "2026-07-22T16:00:00",
  "path": "/api/auth/login"
}
```

### 주요 에러 코드
| code | HTTP | 설명 |
|---|---|---|
| `USER_DUPLICATE_USERNAME` | 409 | 중복 username |
| `AUTH_INVALID_CREDENTIALS` | 401 | 로그인 실패 |
| `COMMON_INVALID_INPUT` | 400 | 입력값 검증 실패 |
| `COMMON_INTERNAL_ERROR` | 500 | 서버 내부 오류 |

## Profiles
| profile | 용도 |
|---|---|
| `local` | 로컬 개발 (기본값) |
| `prod` | 운영 (`ddl-auto: validate`, 에러 메시지 숨김) |

## 프로젝트 구조 (인증)
```
user/
  controller/AuthController   # HTTP 엔드포인트
  service/UserService         # 비즈니스 로직
  entity/User                 # JPA 엔티티
auth/jwt/
  JwtTokenProvider            # 토큰 생성/검증
  JwtAuthenticationFilter     # 요청마다 Bearer 토큰 검사
common/exception/
  GlobalExceptionHandler      # 표준 에러 응답
config/
  SecurityConfig              # Spring Security 설정
  OpenApiConfig               # Swagger 문서 설정
```
