#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$SCRIPT_DIR/docker-compose.yml" ]; then
  PROJECT_DIR="$SCRIPT_DIR"
elif [ -f "$SCRIPT_DIR/../docker-compose.yml" ]; then
  PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
elif [ -f "$SCRIPT_DIR/../docker/docker-compose.yml" ]; then
  PROJECT_DIR="$(cd "$SCRIPT_DIR/../docker" && pwd)"
else
  PROJECT_DIR="$SCRIPT_DIR"
fi

LOG_DIR="${DEPLOY_LOG_DIR:-$PROJECT_DIR/deploy-logs}"
DISCORD_WEBHOOK_URL="${DEPLOY_DISCORD_WEBHOOK_URL:-${DISCORD_DEPLOY_WEBHOOK:-}}"
TAIL_LINES="${DEPLOY_LOG_TAIL_LINES:-200}"

cd "$PROJECT_DIR"

save_failure_artifacts() {
  local reason="$1"
  local timestamp
  local log_prefix
  local log_file
  local inspect_file
  local compose_state_file

  timestamp=$(date +%F-%H%M%S)
  log_prefix="$LOG_DIR/${TARGET_ENV}-${timestamp}"
  log_file="${log_prefix}.log"
  inspect_file="${log_prefix}.inspect.json"
  compose_state_file="${log_prefix}.compose.txt"

  mkdir -p "$LOG_DIR"

  docker logs --timestamps "duit-server-$TARGET_ENV" > "$log_file" 2>&1 || true
  docker inspect "duit-server-$TARGET_ENV" > "$inspect_file" 2>&1 || true
  docker compose --profile "$TARGET_ENV" ps > "$compose_state_file" 2>&1 || true

  echo "배포 실패 사유: $reason"
  echo "전체 로그 저장: $log_file"
  echo "컨테이너 inspect 저장: $inspect_file"
  echo "compose 상태 저장: $compose_state_file"
  echo "최근 로그 ${TAIL_LINES}줄:"
  tail -n "$TAIL_LINES" "$log_file" || true

  if [ -n "$DISCORD_WEBHOOK_URL" ] && [ -s "$log_file" ]; then
    local payload
    payload=$(printf '{"content":"배포 실패: %s 환경 | 사유: %s | 전체 로그 파일을 첨부합니다. 서버 저장 경로: %s"}' \
      "$TARGET_ENV" \
      "$reason" \
      "$log_file")

    curl -fsS \
      -F "payload_json=$payload" \
      -F "file1=@$log_file;type=text/plain" \
      "$DISCORD_WEBHOOK_URL" >/dev/null || echo "Discord 로그 업로드 실패"
  else
    echo "Discord 로그 업로드 건너뜀"
  fi
}

rollback_target_env() {
  echo "롤백: $TARGET_ENV 환경 종료"
  docker compose --profile "$TARGET_ENV" down || true
}

fail_deploy() {
  local reason="$1"

  save_failure_artifacts "$reason"
  rollback_target_env
  exit 1
}

# 1. 현재 환경 감지
CURRENT_ENV="blue"
if docker ps --format "{{.Names}}" | grep -q "duit-server-green"; then
  curl -sf http://localhost:8082/actuator/health >/dev/null && CURRENT_ENV="green"
fi

# 2. 타겟 환경 결정
if [ "$CURRENT_ENV" = "blue" ]; then
  TARGET_ENV="green"
else
  TARGET_ENV="blue"
fi

echo "현재: $CURRENT_ENV → 배포: $TARGET_ENV"

# 3. 컨테이너 시작
if ! docker compose --profile "$TARGET_ENV" up -d; then
  fail_deploy "타겟 환경 컨테이너 시작 실패"
fi

# 4. 헬스체크 (최대 3분)
for i in {1..60}; do
  if docker exec "duit-server-$TARGET_ENV" curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; then
    echo "헬스체크 성공"

    # 5. 트래픽 전환
    if bash "$SCRIPT_DIR/switch-traffic.sh" "$TARGET_ENV"; then
      # 6. 이전 환경 중지
      docker compose --profile "$CURRENT_ENV" down
      echo "배포 완료: $TARGET_ENV"
      exit 0
    fi

    fail_deploy "트래픽 전환 실패"
  fi

  echo "헬스체크 $i/60..."
  sleep 3
done

# 7. 실패 시 롤백
fail_deploy "헬스체크 실패"
