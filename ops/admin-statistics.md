# 관리자 통계 운영 가이드

관리자 통계는 `/api/admin/statistics/**`에서 제공한다. 일반 사용자는 같은 API를 호출해도 `403 Forbidden`을 받는다.

## 통계 기준

- 시간대: `Asia/Seoul`
- DAU: 오늘 로그인하거나 인증된 페이지/API 활동을 남긴 고유 사용자 수
- MAU: 오늘을 포함한 최근 30일 동안 활동한 고유 사용자 수
- 페이지 방문: 로그인한 사용자가 방문한 경로만 기록하며 쿼리 문자열은 저장하지 않는다.
- AI 사용: 일일 AI 한도를 차감한 생성 요청 수를 기록한다. 외부 AI 호출이 이후 실패해도 요청 수에는 포함된다.
- 기존 계정: `V5` 적용 전에 생성된 계정은 정확한 가입일을 알 수 없어 `created_at`을 `NULL`로 유지한다.
- 과거 데이터: `V5` 적용 전의 로그인·페이지 방문·AI 요청·탈퇴 이력은 복원하지 않는다. 적용 이후 이벤트부터 정확히 집계한다.
- 전체 계정: `totalAccounts`는 탈퇴 계정 행까지 포함하며, `activeAccounts`와 `withdrawnAccounts`를 함께 제공한다.

## 최초 관리자 지정

애플리케이션 코드에는 관리자 사용자명을 하드코딩하지 않는다. 관리자도 운영 프런트에서 일반 회원가입으로 먼저 생성한 뒤, 운영자가 정확한 계정 한 개의 역할만 승격한다. 사용자를 SQL로 직접 생성하면 비밀번호 암호화와 가입 이력이 누락되므로 사용하지 않는다.

실행 위치: FIREBAT 홈서버의 백엔드 배포 저장소

```bash
cd /home/hyungyu/services/myenglishvocab-api

git status --short

docker compose \
  --env-file .env.production \
  -f docker-compose.home.yml \
  ps
```

작업 트리가 깨끗하고 애플리케이션·PostgreSQL·Redis가 정상 상태일 때만 진행한다. PostgreSQL 컨테이너로 들어간다.

```bash
docker compose \
  --env-file .env.production \
  -f docker-compose.home.yml \
  exec postgres sh
```

컨테이너 안에서 PostgreSQL에 접속한다.

```bash
psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
```

먼저 `V5` 적용과 후보 계정을 읽기 전용으로 확인한다. `password` 컬럼은 조회하지 않으며 `<ADMIN_USERNAME>`을 실제 대상 username으로 바꾼다.

```sql
SELECT version, description, success
FROM flyway_schema_history
WHERE version = '5';
```

```sql
SELECT id, username, display_name, role, status, created_at, last_login_at
FROM users
WHERE username = '<ADMIN_USERNAME>';
```

마이그레이션이 `success = true`이고 대상이 정확히 한 행이며 `USER`, `ACTIVE`일 때만 진행한다. 조회된 ID와 username을 모두 조건에 넣어 다른 계정이 변경되지 않게 한다.

```sql
BEGIN;

UPDATE users
SET role = 'ADMIN'
WHERE id = <ADMIN_ID>
  AND username = '<ADMIN_USERNAME>'
  AND role = 'USER'
  AND status = 'ACTIVE'
RETURNING id, username, display_name, role, status;
```

반환 행이 정확히 한 개이고 `ADMIN`, `ACTIVE`이면 `COMMIT;`, 행이 없거나 예상과 다르면 `ROLLBACK;`한다. 역할은 다음 인증 요청부터 서버에서 확인하며, 프런트도 앱을 새로 열거나 새로고침할 때 `/api/auth/me`로 현재 역할을 동기화한다. 메뉴가 바로 나타나지 않으면 로그아웃 후 다시 로그인한다.

관리자 권한을 회수할 때도 ID와 username을 함께 확인하고 `ADMIN`, `ACTIVE`인 정확한 한 계정만 `USER`로 변경한다. 운영 문서에는 실제 관리자 username, 사용자 ID, 토큰과 조회 결과를 기록하지 않는다.

## 관리자 대시보드

관리자 계정으로 로그인하면 홈 화면에 **운영 대시보드** 메뉴가 표시된다. 메뉴를 누르거나 프런트의 `/admin` 경로로 이동하면 요약 지표, 일별·월별 흐름, 인기 단어·페이지, 사용자 현황, 가입·탈퇴 이력을 한 화면에서 확인할 수 있다.

일반 사용자가 `/admin`에 직접 접근하면 홈으로 돌아가고, 서버 API도 별도로 `ADMIN` 권한을 검사한다. Swagger UI는 API 응답을 점검하거나 장애를 진단할 때만 사용하면 된다.

## API 목록

| API | 용도 | 범위 제한 |
|---|---|---|
| `GET /api/admin/statistics/overview` | 전체 계정·7일 가입·단어·DAU·MAU·페이지·AI·탈퇴 요약 | 고정 |
| `GET /api/admin/statistics/daily?days=30` | 일별 가입·활동·페이지·AI·탈퇴 | 1~365일 |
| `GET /api/admin/statistics/monthly?months=12` | 월별 가입·활동·페이지·AI·탈퇴 | 1~24개월 |
| `GET /api/admin/statistics/popular-words?limit=20` | 여러 사용자가 많이 저장한 단어 | 1~100개 |
| `GET /api/admin/statistics/popular-pages?days=30&limit=20` | 인증 사용자 페이지 방문 순위 | 1~365일, 1~100개 |
| `GET /api/admin/statistics/users?limit=100` | 가입일·최근 로그인·최근 활동·상태 | 1~500명 |
| `GET /api/admin/statistics/account-lifecycle?limit=100` | 가입·탈퇴 이력 | 1~500건 |

요청에는 로그인 응답으로 받은 Access Token이 필요하다.

```bash
curl -sS \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  https://api.myenglishvocab.com/api/admin/statistics/overview
```

토큰을 셸 히스토리, 문서, 스크린샷에 직접 적지 않는다.

## 회원 탈퇴와 이후의 새 가입

`POST /api/auth/withdraw`는 현재 비밀번호를 다시 확인한다. 성공하면 다음 처리를 수행한다.

- 단어와 퀴즈 데이터 삭제
- 모든 추적 가능한 Refresh Token 삭제
- 계정을 `WITHDRAWN` 상태로 변경하고 관리자 역할 회수
- 탈퇴 이력 기록
- 기존 Access Token의 후속 요청 차단

탈퇴 시 기존 계정은 `WITHDRAWN`으로 영구 유지하고 로그인명을 내부 익명값으로 바꾼다. 탈퇴 전 토큰이 이후 새 계정에서 다시 유효해지지 않게 하기 위한 경계다. 같은 `username`이 다시 사용되더라도 동일인이라고 판단하지 않으며, 별도의 새 계정과 `SIGNUP` 이력으로 기록한다.

## 개인정보와 보관 주의

활동 이벤트에는 단어·뜻·검색어·쿼리 문자열을 넣지 않는다. 페이지 경로, 활동 종류, 사용자 ID, 시각만 저장한다. 사용자명과 최근 로그인 정보가 포함된 관리자 응답은 외부에 공유하거나 공개 로그에 남기지 않는다.

현재 이벤트는 자동 삭제하지 않는다. 실제 운영 정책을 정할 때 개인정보 처리방침과 필요한 분석 기간을 함께 검토하고, 장기 운영 전에는 일별 집계 테이블과 원본 이벤트 보관 기간을 별도로 정한다.
