#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_DIR="/home/ubuntu/myenglishvocab-api"
ENV_FILE="${PROJECT_DIR}/.env.production"
COMPOSE_FILE="${PROJECT_DIR}/docker-compose.prod.yml"
APP_CONTAINER="vocab-prod-server"

MAX_HEALTH_ATTEMPTS=36
HEALTH_INTERVAL_SECONDS=5

if [[ ! -d "${PROJECT_DIR}/.git" ]]; then
  printf 'Git repository does not exist: %s\n' "$PROJECT_DIR" >&2
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  printf 'Production environment file does not exist: %s\n' "$ENV_FILE" >&2
  exit 1
fi

compose() {
  docker compose \
    --env-file "$ENV_FILE" \
    -f "$COMPOSE_FILE" \
    "$@"
}

cd "$PROJECT_DIR"

commit_sha="$(git rev-parse --short HEAD)"
printf 'Deploying commit: %s\n' "$commit_sha"

printf 'Validating production Compose configuration\n'
compose config --quiet

printf 'Building and starting production containers\n'
compose up --build -d

printf 'Waiting for the application container to become healthy\n'

app_status="unknown"

for ((attempt = 1; attempt <= MAX_HEALTH_ATTEMPTS; attempt++)); do
  app_status="$(
    docker inspect \
      --format '{{.State.Health.Status}}' \
      "$APP_CONTAINER" \
      2>/dev/null || true
  )"

  if [[ -z "$app_status" ]]; then
    app_status="not-found"
  fi

  printf 'Health attempt %d/%d: %s\n' \
    "$attempt" \
    "$MAX_HEALTH_ATTEMPTS" \
    "$app_status"

  if [[ "$app_status" == "healthy" ]]; then
    break
  fi

  if [[ "$app_status" == "exited" || "$app_status" == "dead" ]]; then
    break
  fi

  sleep "$HEALTH_INTERVAL_SECONDS"
done

if [[ "$app_status" != "healthy" ]]; then
  printf 'Application container did not become healthy: %s\n' \
    "$app_status" >&2
  exit 1
fi

printf 'Checking the internal Spring Boot health endpoint\n'

compose exec -T app \
  curl -fsS http://localhost:8080/actuator/health

printf '\n'
compose ps

printf 'Deployment succeeded: %s\n' "$commit_sha"
