#!/usr/bin/env bash
set -euo pipefail
umask 077

PROJECT_DIR="/home/ubuntu/myenglishvocab-api"
BACKUP_DIR="/var/backups/myenglishvocab/postgresql"
RETENTION_MINUTES=10080

if [[ ! -d "$BACKUP_DIR" ]]; then
  printf 'backup directory does not exist: %s\n' "$BACKUP_DIR" >&2
  exit 1
fi

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup_file="${BACKUP_DIR}/myenglishvocab_${timestamp}.dump"

if [[ -e "$backup_file" ]]; then
  printf 'backup file already exists: %s\n' "$backup_file" >&2
  exit 1
fi

temp_file="$(mktemp "${BACKUP_DIR}/.myenglishvocab_${timestamp}.XXXXXX.tmp")"

cleanup() {
  if [[ -n "${temp_file:-}" && -e "$temp_file" ]]; then
    rm -f -- "$temp_file"
  fi
}
trap cleanup EXIT

cd "$PROJECT_DIR"

docker compose \
  --env-file .env.production \
  -f docker-compose.prod.yml \
  exec -T postgres \
  sh -c 'exec pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' \
  > "$temp_file"

docker compose \
  --env-file .env.production \
  -f docker-compose.prod.yml \
  exec -T postgres \
  sh -c 'pg_restore -l >/dev/null' \
  < "$temp_file"

mv -- "$temp_file" "$backup_file"

size_bytes="$(stat -c '%s' "$backup_file")"
printf '%s backup succeeded file=%s size=%s bytes\n' \
  "$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
  "$backup_file" \
  "$size_bytes"

printf '%s retention cleanup started threshold_minutes=%s\n' \
  "$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
  "$RETENTION_MINUTES"

find "$BACKUP_DIR" \
  -maxdepth 1 \
  -type f \
  -name 'myenglishvocab_*.dump' \
  -mmin "+${RETENTION_MINUTES}" \
  -printf 'deleting file=%p\n' \
  -delete
