# MyEnglishVocab Server

Spring Boot 기반 영어 단어장 API 서버입니다.  
JWT Access Token + Redis Refresh Token 인증, 사용자별 단어장 API, 표준 에러 응답을 적용했습니다.

## Tech Stack
- Java 21, Spring Boot 4
- Spring Security + JWT (jjwt)
- Spring Data Redis (Refresh Token 저장)
- Spring Data JPA, H2
- SpringDoc OpenAPI
- Docker Compose (로컬 Redis)

## 로컬 실행

Docker Desktop을 먼저 실행한 뒤, `server/` 디렉터리에서 아래를 진행하세요.

### 1) Redis (Docker Compose)
Refresh Token 저장소로 Redis가 필요합니다. Compose로 기동합니다.

```bash
docker compose up -d
```

상태 확인:

```bash
docker compose ps
```

종료 (컨테이너만 중지·삭제, 데이터 volume은 유지):

```bash
docker compose down
```

데이터 volume까지 삭제하려면:

```bash
docker compose down -v
```

> `docker-compose.yml`은 Redis만 포함합니다. 애플리케이션은 아래 `./gradlew bootRun`으로 실행합니다.

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

## 단어장 API

모든 Word API는 **JWT 인증 필수**입니다. URL에 userId를 넣지 않으며, JWT의 `userId`로 본인 단어만 접근합니다.

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/words` | 내 단어 목록 (저장 순서) |
| `GET` | `/api/words/{id}` | 내 단어 단건 조회 |
| `POST` | `/api/words` | 단어 추가 (level은 서버에서 0으로 시작) |
| `PUT` | `/api/words/{id}` | 단어 내용 수정 (level 변경 불가) |
| `DELETE` | `/api/words/{id}` | 단어 삭제 |
| `POST` | `/api/words/{id}/mark-learned` | 외웠음 처리 (level + 1) |

### 단어 생성 예시
```bash
curl -X POST http://localhost:8080/api/words \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "term": "apple",
    "definition": "사과",
    "exampleSentence": "I like apples.",
    "meaningOfExampleSentence": "나는 사과를 좋아한다."
  }'
```

### 퀴즈와의 역할 분리
- **서버**: 단어 목록 제공, `mark-learned`로 level 증가
- **프론트**: 랜덤/순서대로 보여주기, 뜻 보기/넘기기 UI

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
| `USER_NOT_FOUND` | 404 | 사용자 없음 |
| `AUTH_INVALID_CREDENTIALS` | 401 | 로그인 실패 |
| `AUTH_INVALID_REFRESH_TOKEN` | 401 | 유효하지 않은 refresh token |
| `WORD_NOT_FOUND` | 404 | 단어 없음 또는 소유자 아님 |
| `COMMON_INVALID_INPUT` | 400 | 입력값 검증 실패 |
| `COMMON_INTERNAL_ERROR` | 500 | 서버 내부 오류 |

## Profiles
| profile | 용도 |
|---|---|
| `local` | 로컬 개발 (기본값), Redis 사용 |
| `test` | 테스트, InMemory Refresh Token Store |
| `prod` | 운영 (`ddl-auto: validate`, 에러 메시지 숨김) |

## 프로젝트 구조
```
docker-compose.yml            # 로컬 Redis
user/
  controller/AuthController
  service/UserService
  entity/User
word/
  controller/WordController
  service/WordService
  entity/Word
  repository/WordRepository
auth/jwt/
  JwtTokenProvider
  JwtAuthenticationFilter
auth/token/
  RefreshTokenStore
  RedisRefreshTokenStore
  InMemoryRefreshTokenStore
common/exception/
  GlobalExceptionHandler
config/
  SecurityConfig
  OpenApiConfig
```
