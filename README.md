# MyEnglishVocab Server

Spring Boot 기반 영어 단어장 API 서버입니다.  
JWT Access Token + Redis Refresh Token 인증, 사용자별 단어장 API, AI 예문 생성, 표준 에러 응답을 적용했습니다.

로컬에서는 Docker Compose로 백엔드·PostgreSQL·Redis를 함께 실행할 수 있습니다. 운영 환경은 AWS EC2에서 Nginx·Spring Boot·PostgreSQL·Redis를 `docker-compose.prod.yml`로 실행하며, HTTPS와 내부·외부 Health Check를 적용했습니다.

## Tech Stack
- Java 21, Spring Boot 4
- Spring Security + JWT (jjwt)
- Spring Data Redis (Refresh Token · AI 일일 사용량)
- Spring Data JPA, H2, PostgreSQL
- Flyway (DB 스키마 마이그레이션)
- OpenAI / Gemini (예문·뜻 생성, provider 전환 가능)
- SpringDoc OpenAPI
- Spring Boot Actuator (Health Check)
- Docker · Docker Compose (백엔드, Redis, PostgreSQL)
- GitHub Actions CI/CD (PR/`main` 테스트, `main` Merge 후 EC2 자동 배포)

## 로컬 실행

Docker Desktop을 먼저 실행한 뒤, `server/` 디렉터리에서 아래를 진행하세요.

### 1) Redis와 PostgreSQL (Docker Compose)
Refresh Token·AI 일일 사용량에는 Redis가 필요하고, PostgreSQL 프로필 실습에는 PostgreSQL이 필요합니다. Compose로 함께 기동합니다.

```bash
docker compose up -d redis postgres
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

> `docker compose up -d redis postgres`는 Redis와 PostgreSQL만 실행합니다. 애플리케이션은 아래 `./gradlew bootRun`으로 실행합니다. 백엔드까지 컨테이너로 실행하려면 [운영 유사 컨테이너 실행](#운영-유사-컨테이너-실행)을 사용하세요.

### 2) 환경변수
```bash
cp .env.example .env
```

`.env`에서 아래를 설정하세요.  
또는 `application-local.yaml`(gitignore)을 사용해도 됩니다.

| 변수 | 필수인 경우 | 설명 |
|---|---|---|
| `JWT_SECRET` | 항상 | 최소 32자 이상 JWT 서명 키 |
| `SPRING_PROFILES_ACTIVE` | 아니오 | `local`(기본), `postgres`, `prod` |
| `REDIS_HOST` / `REDIS_PORT` | 아니오 | 기본 `localhost` / `6379` |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | `prod` | 운영 PostgreSQL 연결 정보 |
| `CORS_ALLOWED_ORIGINS` | `prod` | 허용할 프론트 origin 목록. 여러 값은 쉼표로 구분 |
| `AI_PROVIDER` | 아니오 | `openai`(기본) 또는 `gemini` |
| `OPENAI_API_KEY` | `AI_PROVIDER=openai` | OpenAI API 키 |
| `GEMINI_ENABLED` / `GEMINI_API_KEY` | `AI_PROVIDER=gemini` | Gemini 활성화 여부와 API 키 |

Spring Boot는 `.env` 파일을 자동으로 읽지 않으므로, 실행 전에 아래 명령으로 현재 터미널의 환경 변수로 불러옵니다.

```bash
set -a
source .env
set +a
```

### 3) 서버 실행
```bash
./gradlew bootRun
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- H2 Console (`local` 프로필): http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./vocabdb`
  - Username: `sa`
  - Password: (비움)

### PostgreSQL 프로필로 실행

Docker PostgreSQL을 사용하려면 `.env`에서 아래처럼 설정합니다.

```dotenv
SPRING_PROFILES_ACTIVE=postgres
```

그 후 환경 변수를 불러오고 서버를 실행합니다.

```bash
set -a
source .env
set +a
./gradlew bootRun
```

첫 실행 시 Flyway가 PostgreSQL에 `users`, `words`, `flyway_schema_history` 테이블을 생성합니다.

### 운영 유사 컨테이너 실행

백엔드·PostgreSQL·Redis를 모두 컨테이너로 실행해 배포 환경과 비슷하게 확인할 수 있습니다.

`.env` 파일은 선택 사항입니다. 새로 clone한 뒤에도 Compose는 로컬 PostgreSQL 프로필과 컨테이너 전용 JWT 기본값으로 실행됩니다. 단, 기본값은 로컬 학습용이므로 실제 운영 배포에 사용하면 안 됩니다.

AI 예문 생성까지 사용하려면 `.env`에 사용할 Provider의 API Key를 설정하세요. `.env` 파일은 Git과 Docker 이미지에 포함되지 않습니다.

그 다음 이미지를 빌드하고 모든 컨테이너를 시작합니다.

```bash
docker compose up --build -d
```

Compose의 `app` 서비스는 기본으로 `postgres` 프로필로 실행됩니다. 컨테이너 내부에서는 `localhost` 대신 서비스 이름인 `postgres`, `redis`로 연결합니다.

실제 운영 설정을 점검하려면 실제 JWT Secret, AI API Key, CORS Origin을 환경 변수로 설정한 뒤 `prod` 프로필을 명시합니다.

```bash
COMPOSE_SPRING_PROFILE=prod docker compose up --build -d
```

`prod` 프로필은 필수 운영 환경 변수가 없으면 시작하지 않습니다. 이 검증은 실제 배포에서 설정 누락을 조기에 발견하기 위한 것입니다.

상태를 확인합니다.

```bash
docker compose ps
```

`vocab-server`, `vocab-postgres`, `vocab-redis`가 모두 `healthy`가 되면 정상입니다.

Health Check는 인증 없이 호출할 수 있습니다.

```bash
curl -i http://localhost:8080/actuator/health
```

정상 응답에는 `status: UP`이 포함됩니다. `groups`는 Spring Boot의 상태 그룹 이름이며, DB·Redis 등의 상세 구성요소는 외부에 노출하지 않습니다.

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

반대로 다른 관리 경로는 인증 없이는 접근할 수 없습니다.

```bash
curl -i http://localhost:8080/actuator
```

이 요청은 `403 Forbidden`이 기대 결과입니다.

앱 로그는 다음 명령으로 확인합니다.

```bash
docker compose logs -f app
```

종료 시 데이터 volume을 유지하려면 아래 명령을 사용합니다.

```bash
docker compose down
```

처음부터 다시 검증해야 할 때만 volume까지 삭제합니다. 이 명령은 PostgreSQL·Redis 데이터를 삭제합니다.

```bash
docker compose down -v
```

## 운영 배포

로컬용 `docker-compose.yml`은 학습과 통합 실행을 위해 PostgreSQL, Redis, Spring Boot 포트를 호스트에 공개합니다. 실제 EC2에서는 이를 사용하지 않고 `docker-compose.prod.yml`을 사용합니다.

운영 Compose는 Nginx만 `80`, `443` 포트를 공개합니다. PostgreSQL(`5432`), Redis(`6379`), Spring Boot(`8080`)는 Docker 내부 네트워크에만 두고, Nginx가 HTTPS 요청을 `app:8080`으로 전달합니다.

### 현재 운영 상태

- 프론트엔드: Vercel (`https://app.myenglishvocab.com`)
- 백엔드: AWS EC2 (`https://api.myenglishvocab.com`)
- 실행 구성: Nginx, Spring Boot, PostgreSQL, Redis
- 공개 포트: `80`, `443`만 사용
- HTTPS: Let's Encrypt 인증서와 Certbot 자동 갱신 적용
- 데이터 보호: PostgreSQL·Redis named Volume 유지, PostgreSQL 정기 백업 적용
- 배포 방식: GitHub OIDC와 AWS Systems Manager Run Command를 이용한 GitHub Actions 자동 배포

`main`에 변경이 반영되면 GitHub Actions가 장기 AWS Access Key 없이 OIDC 임시 권한을 얻어 EC2에 SSM 명령을 보냅니다. EC2는 최신 `main`을 받은 뒤 운영 Compose를 다시 빌드하고, 내부·외부 Health Check를 수행합니다. 운영 비밀값은 EC2의 `.env.production`에 유지하며 PostgreSQL·Redis Volume은 삭제하지 않습니다. SSH `22`번 포트를 GitHub Actions에 공개하거나 EC2를 self-hosted runner로 사용하지 않습니다.

### 운영 환경 변수

운영 비밀값은 Git에 넣지 않습니다. EC2에서만 아래처럼 만듭니다.

```bash
cp .env.production.example .env.production
chmod 600 .env.production
```

`.env.production`에는 강한 DB 비밀번호, 32자 이상의 JWT Secret, 선택한 AI Provider의 API Key를 입력합니다. `CORS_ALLOWED_ORIGINS`에는 정확히 `https://app.myenglishvocab.com`을 설정합니다.

### Nginx와 Certbot HTTPS 흐름

Certbot을 실행하기 전에 Cloudflare DNS에서 `api.myenglishvocab.com`의 `A` 레코드가 EC2 Elastic IP를 가리키고, Proxy status가 `DNS only`인지 확인해야 합니다. Let's Encrypt가 인터넷의 `80` 포트로 ACME 검증 파일에 접근할 수 있어야 하기 때문입니다.

처음에는 `nginx/00-http.conf`만 활성화해 HTTP `80` 포트의 ACME 검증 경로를 제공합니다. Certbot이 `/.well-known/acme-challenge/`에 임시 파일을 만들고, Let's Encrypt가 이를 읽어 `api.myenglishvocab.com`의 소유권을 확인합니다.

저장소에는 현재 운영 중인 `nginx/10-https.conf`가 포함되어 있습니다. 아직 인증서가 없는 새 EC2에서는 이 파일을 잠시 `.disabled`로 바꿔 Nginx가 HTTPS 설정을 읽지 않게 해야 합니다. 인증서 발급 후 원래 이름으로 되돌리면 Nginx가 TLS 설정과 역방향 프록시 설정을 읽습니다. 이미 인증서가 있는 현재 운영 서버에서는 이 초기 비활성화 단계를 다시 실행하지 않습니다.

운영 컨테이너 시작과 인증서 발급은 다음 순서로 진행합니다.

```bash
# 새 EC2에서 최초 인증서를 발급할 때만 HTTPS 설정을 잠시 비활성화
mv nginx/10-https.conf nginx/10-https.conf.disabled

# Nginx(HTTP), Spring Boot, PostgreSQL, Redis 시작
sudo docker compose --env-file .env.production -f docker-compose.prod.yml up --build -d

# api.myenglishvocab.com 인증서 발급
sudo docker compose --env-file .env.production -f docker-compose.prod.yml run --rm certbot \
  certonly --webroot -w /var/www/certbot \
  --email you@example.com --agree-tos --no-eff-email \
  -d api.myenglishvocab.com
```

인증서 발급 뒤에는 HTTPS 설정 파일을 원래 이름으로 되돌리고 Nginx를 재시작합니다. 이름을 되돌린 뒤 `git status --short`가 비어 있으면 저장소도 다시 깨끗한 상태입니다.

```bash
mv nginx/10-https.conf.disabled nginx/10-https.conf
sudo docker compose --env-file .env.production -f docker-compose.prod.yml restart nginx
```

인증서는 Certbot과 Cron으로 자동 갱신하며, `certbot renew --dry-run`으로 갱신 절차를 검증했습니다.

### 수동 재배포와 장애 대응

자동 배포를 다시 실행할 수 없거나 운영 상태를 직접 점검해야 할 때는 EC2의 백엔드 저장소에서 아래 순서로 수동 재배포할 수 있습니다.

```bash
git pull --ff-only origin main

sudo docker compose \
  --env-file .env.production \
  -f docker-compose.prod.yml \
  up --build -d
```

`git pull --ff-only`는 원격 `main`과 선형으로 이어질 때만 변경 사항을 받습니다. EC2에 예상하지 못한 로컬 커밋이 있으면 자동으로 합치지 않고 실패하므로 운영 저장소의 변경을 조기에 발견할 수 있습니다.

`up --build -d`는 애플리케이션 이미지를 다시 빌드하고 필요한 컨테이너를 갱신하지만 named Volume은 삭제하지 않습니다. 운영 데이터가 들어 있는 `postgres-prod-data`, `redis-prod-data`를 보호하기 위해 운영에서는 `docker compose down -v`를 실행하지 않습니다.

배포 후 컨테이너 상태와 내부 Health Check를 확인합니다. Spring Boot의 `8080` 포트는 호스트에 공개하지 않았으므로 내부 확인은 `app` 컨테이너 안에서 실행합니다.

```bash
sudo docker compose \
  --env-file .env.production \
  -f docker-compose.prod.yml \
  ps

sudo docker compose \
  --env-file .env.production \
  -f docker-compose.prod.yml \
  exec app curl -fsS http://localhost:8080/actuator/health

curl -fsS https://api.myenglishvocab.com/actuator/health
```

정상 응답에는 `"status":"UP"`이 포함됩니다.

자동 배포 직후 문제가 생겨 긴급하게 이전 코드를 실행해야 한다면 작업 트리가 깨끗한지 확인한 뒤 이전 Commit을 임시로 Checkout하고 같은 Compose 명령으로 다시 빌드할 수 있습니다.

```bash
cd /home/ubuntu/myenglishvocab-api
git status --short
git log --oneline -5
git switch --detach <PREVIOUS_COMMIT>

sudo docker compose \
  --env-file .env.production \
  -f docker-compose.prod.yml \
  up --build -d
```

이 방식은 긴급 복구용이며, 이번 자동 배포 작업에서는 운영 서버를 이전 Commit으로 되돌릴 이유가 없어 끝까지 실행해 검증하지는 않았습니다. 문제를 해결한 뒤에는 문제가 된 Commit을 되돌리는 Revert Pull Request를 `main`에 반영하는 편이 운영 이력을 보존하는 데 안전합니다. EC2를 다시 현재 `main`으로 돌릴 때는 다음 명령을 사용합니다.

```bash
git switch main
git pull --ff-only origin main

sudo docker compose \
  --env-file .env.production \
  -f docker-compose.prod.yml \
  up --build -d
```

DB Migration에 컬럼 삭제나 의미 변경이 포함됐다면 코드만 이전 버전으로 되돌릴 수 있는지 별도로 확인해야 합니다. 복구 과정에서도 PostgreSQL·Redis Volume을 삭제하는 `docker compose down -v`는 사용하지 않습니다.

### 4) 테스트
```bash
./gradlew test --rerun-tasks
```

테스트 프로필(`test`)에서는 Redis 대신 인메모리 Refresh Token Store · AI 사용량 Limiter를 사용합니다. `OperationsEndpointTest`는 운영 프로필에서 `/actuator/health`만 공개되고 상세 상태가 숨겨지는지, Spring Security의 기본 사용자가 자동 생성되지 않는지도 검증합니다. JPA의 `open-in-view`는 모든 프로필에서 `false`로 두어 요청 처리 중 영속성 컨텍스트가 불필요하게 유지되지 않도록 했습니다.

## CORS

브라우저에서 프론트엔드가 API를 호출하려면 CORS가 필요합니다.  
기본으로 로컬 프론트 Origin을 허용합니다.

```yaml
cors:
  allowed-origins:
    - http://localhost:3000   # Next.js
    - http://localhost:5173   # Vite
```

운영에서는 `application-prod.yaml`이 `CORS_ALLOWED_ORIGINS` 환경 변수를 읽습니다.

```dotenv
CORS_ALLOWED_ORIGINS=https://myenglishvocab.example.com
```

여러 프론트 주소를 허용해야 한다면 쉼표로 구분합니다.

```dotenv
CORS_ALLOWED_ORIGINS=https://app.example.com,https://www.example.com
```

`*`로 모든 origin을 허용하지 않습니다. 이 서버는 refresh token 쿠키를 사용하므로, 허용할 프론트 주소를 명시해야 합니다.

## Refresh Token Cookie 배포 정책

Refresh token은 JavaScript가 읽을 수 없는 httpOnly 쿠키로 저장합니다. 현재 정책은 다음과 같습니다.

| 속성 | 현재 값 | 의미 |
|---|---|---|
| `HttpOnly` | `true` | JavaScript로 token 값을 읽을 수 없음 |
| `Secure` | local: `false`, prod: `true` | 운영에서는 HTTPS 연결에서만 쿠키 전송 |
| `SameSite` | `Lax` | 같은 site 요청에서만 기본적으로 쿠키 전송 |
| `Path` | `/api/auth` | refresh·logout 등 인증 API에만 쿠키 전송 |

현재 운영 주소인 `app.myenglishvocab.com`과 `api.myenglishvocab.com`은 같은 site로 취급되므로 이 정책과 맞습니다. 프론트는 refresh·logout 요청에 반드시 `credentials: include`를 설정해야 합니다.

```ts
fetch("https://api.myenglishvocab.com/api/auth/refresh", {
  method: "POST",
  credentials: "include"
});
```

프론트와 API를 완전히 다른 site로 옮긴다면(예: Vercel 기본 도메인과 별도 API 도메인), 현재 `SameSite=Lax` 정책으로는 refresh cookie가 전송되지 않을 수 있습니다. 그때는 `SameSite=None`과 `Secure=true`로 변경하고, CORS allowlist·CSRF 위험·브라우저의 third-party cookie 제한을 함께 검토해야 합니다.

## 운영 배포 설정

`prod` 프로필은 H2 Console과 Swagger UI/API Docs를 비활성화합니다. 시작 시 다음 값을 검증합니다.

- `JWT_SECRET`: 32자 이상
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: 운영 PostgreSQL 연결 정보
- `CORS_ALLOWED_ORIGINS`: 실제 프론트 origin
- `AI_PROVIDER=openai`: `OPENAI_API_KEY`
- `AI_PROVIDER=gemini`: `GEMINI_ENABLED=true`, `GEMINI_API_KEY`, Gemini model

운영 Compose용 `.env.production` 예시는 다음과 같습니다. `SPRING_PROFILES_ACTIVE`, `DB_URL`, `REDIS_HOST`, `REDIS_PORT`는 `docker-compose.prod.yml`이 컨테이너 내부 값으로 설정하므로 이 파일에 중복해서 넣지 않습니다.

```dotenv
CORS_ALLOWED_ORIGINS=https://app.myenglishvocab.com
DB_USERNAME=vocab_app
DB_PASSWORD=change-me-to-a-long-random-password
JWT_SECRET=change-me-to-a-long-random-secret-at-least-32-characters
AI_PROVIDER=openai
OPENAI_API_KEY=your-openai-api-key
GEMINI_ENABLED=true
GEMINI_API_KEY=
```

프론트와 API는 `app.myenglishvocab.com`, `api.myenglishvocab.com`처럼 같은 최상위 도메인의 HTTPS 주소를 사용합니다. EC2에서 PostgreSQL `5432`, Redis `6379`, Spring Boot `8080` 포트는 인터넷에 직접 공개하지 않고, 외부 요청은 HTTPS가 적용된 Nginx를 통해 전달합니다.

배포 후에는 `/actuator/health`뿐 아니라 회원가입, 로그인, 새로고침 후 로그인 복구, 로그아웃, 단어 CRUD, AI 생성과 퀴즈까지 Smoke Test를 진행합니다.

## CI/CD

### CI

GitHub Actions가 `main`에 대한 push와 pull request마다 테스트를 실행합니다.

- 워크플로: `.github/workflows/ci.yml`
- 실행 내용: JDK 21 설정 후 `./gradlew test --no-daemon`
- Redis는 테스트 프로필에서 InMemory Store를 쓰므로 CI에 Redis가 필요하지 않습니다.

PR 페이지의 **Checks / Actions** 탭에서 결과를 확인할 수 있습니다.

### CD

- 워크플로: `.github/workflows/deploy.yml`
- 실행 조건: `main` push 또는 수동 `workflow_dispatch`
- 인증: GitHub OIDC로 배포 전용 IAM Role의 임시 권한 획득
- 명령 전달: AWS Systems Manager Run Command
- EC2 작업: 깨끗한 작업 트리 확인 → `main` 갱신 → `ops/deploy-production.sh`
- 배포 작업: `docker compose up --build -d` → 내부 Health Check
- Actions 작업: SSM 출력 수집 → 외부 Health Check
- 실패 처리: SSM으로 관련 Docker 상태와 로그를 다시 수집해 Actions Log에 출력

GitHub 저장소에는 AWS 장기 Access Key나 `.env.production`을 저장하지 않습니다. IAM 권한은 대상 EC2에 `AWS-RunShellScript`를 보내고 해당 명령 결과를 조회하는 데 필요한 SSM 작업으로 제한합니다.

## DB 마이그레이션 (Flyway)

스키마 변경은 Hibernate `ddl-auto`가 아니라 **Flyway SQL**로 관리합니다.

- 마이그레이션 위치: `src/main/resources/db/migration/`
  - `V1__create_users.sql` — users 테이블
  - `V2__create_words.sql` — words 테이블
  - `V3__add_favorite_to_words.sql` — 기존 단어와 호환되는 즐겨찾기 컬럼 추가
- 앱 기동 시 Flyway가 아직 적용되지 않은 버전만 순서대로 실행합니다.
- JPA `ddl-auto`는 `validate`입니다. (엔티티와 DB 스키마 일치만 검사)
- 적용 이력은 현재 사용하는 DB(H2 또는 PostgreSQL)의 `flyway_schema_history` 테이블에서 확인할 수 있습니다.

로컬에서 스키마를 처음부터 다시 적용하려면 서버를 끈 뒤 H2 파일을 삭제하세요.

```bash
rm -f vocabdb.mv.db vocabdb.trace.db
```

그다음 앱을 다시 실행하면 V1부터 V3까지 다시 적용됩니다. (로컬 데이터는 삭제됩니다.)

## 인증 흐름
1. `POST /api/auth/signup` — 회원가입
2. `POST /api/auth/login` — body에 `accessToken`, **httpOnly 쿠키**에 `refreshToken`
3. `Authorization: Bearer {accessToken}` 헤더로 보호 API 호출
4. Access 만료 시 `POST /api/auth/refresh` — 쿠키의 refresh로 재발급 (RTR, Set-Cookie로 새 refresh)
5. `POST /api/auth/logout` — Redis refresh 삭제 + 쿠키 만료
6. `GET /api/auth/me` — 인증된 사용자 정보 조회

### Swagger UI에서 Bearer 인증하기

1. `POST /api/auth/login`을 실행하고 응답 body의 `accessToken` 값을 복사합니다.
2. Swagger UI 오른쪽 위의 **Authorize** 버튼을 누릅니다.
3. `bearerAuth` 입력칸에 **`Bearer`를 제외한 accessToken 값만** 붙여넣습니다.
4. Authorize를 누른 뒤 보호 API를 호출합니다.

Swagger의 HTTP Bearer 보안 방식은 `Authorization: Bearer {accessToken}` 헤더를 자동으로 만듭니다. 따라서 입력칸에 `Bearer `까지 넣으면 `Bearer Bearer ...`가 되어 인증에 실패할 수 있습니다.

브라우저/프론트는 refresh·logout 시 `credentials: include`로 쿠키를 전송합니다. Bruno는 쿠키 jar를 켜면 됩니다.

### 토큰 역할
| 토큰 | 수명(기본) | 저장 | 용도 |
|---|---|---|---|
| Access Token (JWT) | 30분 | 클라이언트 메모리 | API 인증 (Bearer) |
| Refresh Token (UUID) | 7일 | Redis + **httpOnly 쿠키** | Access 재발급 / 로그아웃 |

## 단어장 API

모든 Word API는 **JWT 인증 필수**입니다. URL에 userId를 넣지 않으며, JWT의 `userId`로 본인 단어만 접근합니다.

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/words` | 내 단어 목록 (저장 순서) |
| `GET` | `/api/words/{id}` | 내 단어 단건 조회 |
| `POST` | `/api/words` | 단어 추가 (level은 0, favorite은 false로 시작) |
| `PUT` | `/api/words/{id}` | 단어 내용 수정 (level 변경 불가) |
| `DELETE` | `/api/words/{id}` | 단어 삭제 |
| `POST` | `/api/words/{id}/mark-learned` | 외웠음 처리 (level + 1) |
| `PATCH` | `/api/words/{id}/favorite` | 즐겨찾기 상태 변경 |
| `POST` | `/api/words/generate-example` | AI로 뜻·예문·해석 생성 (저장하지 않음) |

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

### 즐겨찾기 변경 예시

현재 상태를 반대로 뒤집는 toggle 요청이 아니라 원하는 상태를 명시합니다. 같은 요청을 다시 보내도 결과가 달라지지 않습니다.

```bash
curl -X PATCH http://localhost:8080/api/words/{id}/favorite \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{ "favorite": true }'
```

### 퀴즈와의 역할 분리
- **서버**: 단어 목록 제공, `mark-learned`로 level 증가
- **프론트**: 랜덤/순서대로 보여주기, 뜻 보기/넘기기 UI

## AI 예문 생성

`POST /api/words/generate-example`은 JWT 인증이 필요합니다.  
결과는 DB에 저장되지 않으므로, 확인 후 `POST /api/words`로 저장하세요.

### 동작
1. `term` 필수, `definition` 선택
2. `definition`이 없으면 영어 단어를 짧은 한국어 뜻으로 번역
3. 미국 일상 회화 느낌의 예문 + 한국어 해석 생성
4. 계정당 하루 사용량 차감 (`ai.daily-limit`, 기본 10회, 한국 시간 기준)

### Provider 전환
환경 변수 `AI_PROVIDER`로 전환합니다. 변경 후 서버를 재시작하세요.

```dotenv
AI_PROVIDER=gemini
GEMINI_ENABLED=true
GEMINI_API_KEY=your-gemini-api-key
```

| provider | 필요 키 | 구현 |
|---|---|---|
| `openai` (기본) | `OPENAI_API_KEY` | OpenAI Chat Completions |
| `gemini` | `GEMINI_API_KEY` | Google Gemini generateContent |

### 요청 / 응답 예시
```bash
curl -X POST http://localhost:8080/api/words/generate-example \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{ "term": "apple" }'
```

```json
{
  "definition": "사과",
  "exampleSentence": "I bought a fresh apple from the market.",
  "meaningOfExampleSentence": "나는 시장에서 신선한 사과를 샀다."
}
```

### Redis 키 (참고)
| 키 | 값 | 설명 |
|---|---|---|
| `refresh:{uuid}` | userId | Refresh Token |
| `ai:daily:{userId}:{yyyy-MM-dd}` | 사용 횟수 | AI 일일 한도 (당일 자정까지 TTL) |

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
| `AI_GENERATION_FAILED` | 502 | AI 생성/번역 실패 |
| `AI_NOT_CONFIGURED` | 503 | API 키 미설정 |
| `AI_DAILY_LIMIT_EXCEEDED` | 429 | 하루 AI 사용량 초과 |
| `COMMON_INVALID_INPUT` | 400 | 입력값 검증 실패 |
| `COMMON_INTERNAL_ERROR` | 500 | 서버 내부 오류 |

## Profiles
| profile | 용도 |
|---|---|
| `local` | 로컬 개발 기본값, 파일 기반 H2 + Redis |
| `postgres` | 로컬 Docker PostgreSQL + Redis |
| `test` | 메모리 H2, InMemory Refresh Token·AI Limiter |
| `prod` | 환경 변수 PostgreSQL, H2·Swagger 비활성화, 오류 메시지 숨김 |

## 프로젝트 구조
```
.github/workflows/ci.yml      # GitHub Actions CI
.github/workflows/deploy.yml  # GitHub OIDC + AWS SSM 운영 CD
docker-compose.yml            # 로컬 Redis, PostgreSQL, Spring Boot
docker-compose.prod.yml       # EC2 운영 Compose
ops/deploy-production.sh      # EC2 재빌드와 내부 Health Check
src/main/resources/db/migration/
  V1__create_users.sql
  V2__create_words.sql
  V3__add_favorite_to_words.sql
user/
  controller/AuthController
  service/UserService
  entity/User
word/
  controller/WordController
  service/WordService
  entity/Word
  repository/WordRepository
ai/
  ExampleGenerator / OpenAiExampleGenerator / GeminiExampleGenerator
  translation/ (Translator, OpenAI/Gemini)
  quota/ (Redis · InMemory AiUsageLimiter)
  service/ExampleGenerationService
  config/AiProperties
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
  CorsConfig
  CorsProperties
  OpenApiConfig
  RestClientConfig
```
