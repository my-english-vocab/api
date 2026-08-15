# PostgreSQL 운영 백업

이 디렉터리는 운영 PostgreSQL 백업 자동화의 Git 관리 원본이다.

- `postgres-backup.sh`: `pg_dump`로 사용자 지정 형식(`.dump`) 백업을 만들고, 아카이브 검증과 7일 보관 정책을 수행한다.
- `postgres-backup.cron`: EC2 Cron 등록 원본이다.
- 실제 백업 파일, 로그 파일, `.env.production`은 Git에 저장하지 않는다.

## EC2 설치

PR을 병합한 뒤 EC2에서 최신 코드를 받은 다음 실행한다.

```bash
cd /home/ubuntu/myenglishvocab-api

sudo install -d -o root -g root -m 700 \
  /var/backups/myenglishvocab/postgresql

sudo install -o root -g root -m 700 \
  ops/postgres-backup.sh \
  /usr/local/sbin/myenglishvocab-postgres-backup

sudo install -o root -g root -m 644 \
  ops/postgres-backup.cron \
  /etc/cron.d/myenglishvocab-postgres-backup

sudo touch /var/log/myenglishvocab-postgres-backup.log
sudo chown root:root /var/log/myenglishvocab-postgres-backup.log
sudo chmod 600 /var/log/myenglishvocab-postgres-backup.log
```

EC2 서버 시간은 UTC다. Cron은 매일 `18:00 UTC`, 즉 한국 시간 `03:00 KST`에 실행된다.

## 검증

```bash
sudo /usr/local/sbin/myenglishvocab-postgres-backup
sudo tail -n 20 /var/log/myenglishvocab-postgres-backup.log
```

성공 로그에는 `backup succeeded`와 `retention cleanup started`가 포함된다.

## 안전 경계

- `postgres-prod-data` Docker Volume은 삭제하지 않는다.
- `docker compose down -v`를 사용하지 않는다.
- 복구 검증은 운영 DB가 아닌 별도 테스트 DB에서만 수행한다.
- 7일이 지난 `myenglishvocab_*.dump` 파일만 자동 삭제한다.
