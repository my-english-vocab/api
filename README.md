# MyEnglishVocab Server

Spring Boot 기반 영어 단어장 API 서버입니다.  
JWT Access Token + Redis Refresh Token 인증과 표준 에러 응답을 적용했습니다.

## Tech Stack
- Java 21, Spring Boot 4
- Spring Security + JWT (jjwt)
- Spring Data Redis (Refresh Token 저장)
- Spring Data JPA, H2
- SpringDoc OpenAPI

## 로컬 실행

### 1) Redis
Refresh Token 저장소로 Redis가 필요합니다.

```bash
docker run -d --name vocab-redis -p 6379:6379 redis:7
```

### 2) 환경변수
```bash
cp .env.example .env
```

`.env`에서 `JWT_SECRET`을 32자 이상으로 설정하세요.  
또는 `application-local.yaml`(gitignore)을 사용해도 됩니다.

```bash
export JWT_SECRET=local-dev-secret-key-must-be-at-least-32-characters-long
export SPRING_PROFILES_ACTIVE=local
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

### 3) 서버 실행
```bash
./gradlew bootRun
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- H2 Console: http://localhost:8080/h2-console  
  - JDBC URL: `jdbc:h2:file:./vocabdb`
  - Username: `sa`
  - Password: (비움)

### 4) 테스트
```bash
./gradlew test
```

테스트 프로필(`test`)에서는 Redis 대신 인메모리 Refresh Token Store를 사용합니다.

## 인증 흐름
1. `POST /api/auth/signup` — 회원가입
2. `POST /api/auth/login` — `accessToken` + `refreshToken` 발급
3. `Authorization: Bearer {accessToken}` 헤더로 보호 API 호출
4. Access 만료 시 `POST /api/auth/refresh` — 새 토큰 쌍 발급 (기존 refresh는 폐기, rotation)
5. `POST /api/auth/logout` — Refresh Token 무효화
6. `GET /api/auth/me` — 인증된 사용자 정보 조회

Swagger UI에서 **Authorize** 버튼으로 Bearer accessToken을 등록한 뒤 보호 API를 호출할 수 있습니다.  
refresh / logout은 body에 `refreshToken`을 넣습니다.

### 토큰 역할
| 토큰 | 수명(기본) | 저장 | 용도 |
|---|---|---|---|
| Access Token (JWT) | 30분 | 클라이언트 | API 인증 |
| Refresh Token (UUID) | 7일 | Redis (`refresh:{token}` → userId) | Access 재발급 / 로그아웃 |

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
| `AUTH_INVALID_REFRESH_TOKEN` | 401 | 유효하지 않은 refresh token |
| `COMMON_INVALID_INPUT` | 400 | 입력값 검증 실패 |
| `COMMON_INTERNAL_ERROR` | 500 | 서버 내부 오류 |

## Profiles
| profile | 용도 |
|---|---|
| `local` | 로컬 개발 (기본값), Redis 사용 |
| `test` | 테스트, InMemory Refresh Token Store |
| `prod` | 운영 (`ddl-auto: validate`, 에러 메시지 숨김) |

## 프로젝트 구조 (인증)
```
user/
  controller/AuthController   # HTTP 엔드포인트
  service/UserService         # 회원가입/로그인/refresh/logout
  entity/User                 # JPA 엔티티
auth/jwt/
  JwtTokenProvider            # Access Token 생성/검증
  JwtAuthenticationFilter     # 요청마다 Bearer 토큰 검사
auth/token/
  RefreshTokenStore           # Refresh 저장 포트
  RedisRefreshTokenStore      # Redis 구현 (!test)
  InMemoryRefreshTokenStore   # 테스트용 구현 (test)
common/exception/
  GlobalExceptionHandler      # 표준 에러 응답
config/
  SecurityConfig              # Spring Security 설정
  OpenApiConfig               # Swagger 문서 설정
```
