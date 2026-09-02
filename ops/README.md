# PostgreSQL 운영 백업

현재 운영 백업은 FIREBAT 홈서버의 systemd service와 timer로 관리한다. 이 디렉터리의 `postgres-backup.sh`와 `postgres-backup.cron`은 이전 AWS EC2 운영 방식의 레거시 원본이며 현재 홈서버에서 실행하지 않는다.

- 현재 스크립트: `/home/hyungyu/infra/backup-myenglishvocab-postgres.sh`
- 백업 경로: `/home/hyungyu/backups/myenglishvocab/postgres`
- 실행 시각: 매일 `18:00 UTC`(한국 시간 `03:00`)
- 형식: `pg_dump` custom format
- 정리: 보관 기간이 지난 백업 자동 삭제
- 검증 상태: 별도 테스트 DB를 만든 뒤 `pg_restore` 복원 성공 확인

실제 백업 파일, 로그 파일, `.env.production`과 secret 값은 Git에 저장하지 않는다. 관리자 지정, 통계 기준과 관리자 API 운영 방법은 [`admin-statistics.md`](admin-statistics.md)를 참고한다.

## 현재 상태 확인

systemd unit의 정확한 이름은 홈서버에서 확인하고, 확인된 unit만 조회한다.

```bash
systemctl list-timers --all | grep -i myenglishvocab
systemctl list-units --type=service | grep -i myenglishvocab
```

백업 디렉터리의 파일명·크기·시각만 확인한다. dump 내용을 출력하거나 운영 DB에 복원하지 않는다.

```bash
find /home/hyungyu/backups/myenglishvocab/postgres \
  -maxdepth 1 -type f -name '*.dump' -printf '%TY-%Tm-%Td %TH:%TM %s %f\n' \
  | sort
```

`pg_restore -l`은 아카이브를 읽을 수 있다는 확인일 뿐 복구 성공 증거는 아니다. 실제 복원 검증은 운영 DB와 분리된 테스트 DB에서 수행하고 테이블과 주요 데이터까지 확인해야 한다.

## 안전 경계

- `postgres-prod-data` Docker Volume은 삭제하지 않는다.
- `docker compose down -v`를 사용하지 않는다.
- 복구 검증은 운영 DB가 아닌 별도 테스트 DB에서만 수행한다.
- 7일이 지난 `myenglishvocab_*.dump` 파일만 자동 삭제한다.
