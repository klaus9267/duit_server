#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

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
docker compose --profile $TARGET_ENV up -d

# 4. 헬스체크 (최대 3분)
for i in {1..60}; do
  if docker exec duit-server-$TARGET_ENV curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; then
    echo "헬스체크 성공"

    # 5. 트래픽 전환
    if "switch-traffic.sh" $TARGET_ENV; then
      # 6. 이전 환경 중지
      docker compose --profile $CURRENT_ENV down
      echo "배포 완료: $TARGET_ENV"
      exit 0
    else
      echo "트래픽 전환 실패, 롤백"
      docker compose --profile $TARGET_ENV down
      exit 1
    fi
  fi
  echo "헬스체크 $i/60..."
  sleep 3
done

# 7. 실패 시 롤백
echo "헬스체크 실패, 롤백"
docker compose --profile $TARGET_ENV down
exit 1
