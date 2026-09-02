# 홈서버 운영 기준

> 기준일: 2026-09-02

이 문서는 AWS EC2에서 FIREBAT 홈서버로 이전을 마친 MyEnglishVocab API의 현재 운영 기준이다. 실제 token, password, API key, JWT secret과 `.env.production` 내용은 기록하지 않는다.

## 핵심 상태

- 하드웨어: FIREBAT T8 Pro Plus, Intel N100, RAM 16GB, SSD 512GB
- OS: Ubuntu Server 24.04 LTS, UTC, headless 운영
- 공개 서비스: HTTP `80`, HTTPS `443`만 공유기에서 포트포워딩
- 관리 접근: SSH `22`는 인터넷에 공개하지 않고 현재 LAN에서만 사용
- 프론트엔드: Vercel, `https://app.myenglishvocab.com`
- 백엔드: 홈서버, `https://api.myenglishvocab.com`
- 상태 확인: `https://api.myenglishvocab.com/actuator/health`

AWS의 MyEnglishVocab EC2, Elastic IP, EBS, Security Group, IAM Role, GitHub AWS OIDC Provider와 잔여 비용 리소스 정리는 완료했다. AWS production 배포는 현재 사용하지 않는다.

## 디렉터리와 역할

```text
/home/hyungyu/infra/
├── traefik/                           # 공개 HTTP/HTTPS와 TLS
├── monitoring/                        # Prometheus, Grafana, exporters
├── cloudflare-ddns/                   # 동적 공인 IP의 DNS 갱신
└── backup-myenglishvocab-postgres.sh  # PostgreSQL 백업

/home/hyungyu/services/
└── myenglishvocab-api/                # API 배포 checkout과 .env.production

/home/hyungyu/backups/myenglishvocab/
└── postgres/                           # custom-format dump
```

`/home/hyungyu/actions-runner`에는 GitHub Actions self-hosted runner가 설치되어 있다. runner와 DDNS·백업 작업은 systemd가 관리한다.

## 네트워크와 HTTPS

```text
사용자 브라우저
→ Cloudflare DNS (DNS only)
→ 동적 공인 IP
→ 공유기 80/443 포트포워딩
→ Traefik v3
→ proxy Docker network
→ Spring Boot 8080
```

- Cloudflare DDNS timer가 약 5분마다 공인 IP를 확인해 A 레코드를 갱신한다.
- Traefik은 Cloudflare DNS-01 challenge로 Let's Encrypt 인증서를 관리한다.
- `docker-compose.home.yml`의 Spring Boot, PostgreSQL `5432`, Redis `6379`는 호스트 포트를 publish하지 않는다.
- 공개 Traefik router는 `/actuator/prometheus` 요청을 제외한다. Prometheus만 내부 Docker network에서 이 endpoint를 수집한다.
- 운영 secret은 홈서버 `.env.production`에만 두고 Git, Actions 설정, 로그와 문서에 남기지 않는다.

## 배포

```text
main push 또는 수동 실행
→ GitHub-hosted runner에서 Docker image build
→ ghcr.io/my-english-vocab/api:sha-<commit> push
→ 홈서버 self-hosted runner
→ commit checkout과 image pull
→ docker-compose.home.yml의 app 서비스 교체
→ 외부 Actuator Health Check
```

현재 Workflow는 `CI`와 `Home Server Deploy`다. 이전 AWS `Backend Deploy`와 임시 `Home Server Runner Test`는 제거했다. 홈서버에서는 운영 이미지를 직접 build하지 않는다.

## 데이터와 백업

- PostgreSQL 17과 Redis 7은 named Volume을 사용한다.
- AWS PostgreSQL dump를 홈서버 PostgreSQL에 복원했고 주요 데이터를 확인했다.
- Redis는 새 인스턴스로 시작했으므로 이전 당시 기존 refresh login 상태는 유지하지 않았다.
- 백업 timer는 매일 `18:00 UTC`(한국 시간 `03:00`)에 `pg_dump` custom-format 백업을 만들고 오래된 백업을 정리한다.
- 별도 테스트 DB에 `pg_restore`하고 데이터를 확인하는 복원 검증을 완료했다.
- 운영 중 `docker compose down -v`를 실행하거나 운영 DB에 복원 테스트를 하지 않는다.

## 모니터링과 복구

- `/home/hyungyu/infra/monitoring`에서 Prometheus, Grafana, node-exporter, cAdvisor를 Docker로 운영한다.
- Grafana만 LAN의 `3000` 포트로 접근하고 Prometheus와 exporter 포트는 호스트에 공개하지 않는다.
- Grafana에는 Node Exporter Full, cAdvisor, JVM Metrics, Vocab Server Overview 대시보드가 있다.
- 현재 alerting은 구성하지 않았다.
- 재부팅과 완전 종료 후 수동 전원 켜기에서 Traefik, API 스택, 모니터링, DDNS·백업 timer, Actions runner의 자동 복구를 확인했다.
- 정전 복구 후 자동 전원 켜기를 위한 BIOS의 Restore on AC Power Loss 설정은 아직 적용하지 않았다.

## 운영 검증 완료 범위

- 외부 HTTPS와 Actuator `UP`
- 로그인, 로그아웃, refresh token
- 단어 CRUD
- AI 기능
- PostgreSQL 데이터 이전과 별도 테스트 DB 복원
- GitHub Actions를 통한 실제 홈서버 배포
- 재부팅과 콜드 부팅 후 자동 서비스 복구

## 다음 작업

1. Tailscale로 외부 SSH와 Grafana 관리 접근을 구성한다. SSH `22`를 인터넷에 공개하지 않고 기존 공개 `80`, `443` 서비스에 영향을 주지 않는다.
2. Grafana Loki와 2026년 현재 권장 collector를 이용해 Traefik·Spring Boot 로그를 먼저 수집한다. 로그 구성요소는 인터넷에 공개하지 않는다.
3. 현장 작업이 가능할 때 BIOS의 Restore on AC Power Loss를 설정하고 실제 정전 복구를 검증한다.

운영 변경은 먼저 상태와 로그를 확인한 뒤 한 단계씩 적용한다. 정상 동작 중인 구성은 문제 근거 없이 변경하지 않는다.
