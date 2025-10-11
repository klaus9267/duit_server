#!/bin/bash
# SSH 터널 상태 확인 스크립트 (Windows Git Bash/macOS/Linux)

# 프로젝트 루트에서 실행되는지 확인
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/docker/.env"

# .env 파일 로드
if [ -f "$ENV_FILE" ]; then
    set -a
    source "$ENV_FILE"
    set +a
else
    echo "❌ .env 파일을 찾을 수 없습니다: $ENV_FILE"
    exit 1
fi

# 환경변수에서 설정 읽기
SERVER_IP="${SSH_SERVER_IP}"
LOCAL_PORT="${SSH_LOCAL_PORT}"
REMOTE_HOST="${SSH_REMOTE_HOST}"
REMOTE_PORT="${SSH_REMOTE_PORT}"
SERVER_PORT="${SSH_SERVER_PORT}"

echo "🔍 SSH 터널 상태 확인"
echo ""

# 포트가 리스닝 중인지 확인 (더 신뢰할 수 있는 방법)
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
    # Windows
    PORT_CHECK=$(netstat -ano 2>/dev/null | grep ":${LOCAL_PORT}" | grep "LISTENING")
elif [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    PORT_CHECK=$(lsof -i :${LOCAL_PORT} 2>/dev/null | grep "LISTEN")
else
    # Linux
    PORT_CHECK=$(netstat -tuln 2>/dev/null | grep ":${LOCAL_PORT}" | grep "LISTEN")
fi

if [ -n "$PORT_CHECK" ]; then
    echo "✅ SSH 터널 실행 중"
    echo "   localhost:${LOCAL_PORT} → ${SERVER_IP} (${REMOTE_HOST}:${REMOTE_PORT})"
    echo ""

    # SSH 프로세스 정보 표시 (가능한 경우)
    if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
        # Windows: PID 추출
        PID=$(echo "$PORT_CHECK" | awk '{print $NF}' | head -n 1)
        [ -n "$PID" ] && echo "   PID: $PID (Windows)"
    else
        # macOS/Linux: 프로세스 정보
        PIDS=$(pgrep -f "ssh -p ${SERVER_PORT}" 2>/dev/null)
        for PID in $PIDS; do
            echo "   PID: $PID"
            if [[ "$OSTYPE" == "darwin"* ]]; then
                START_TIME=$(ps -p $PID -o lstart= 2>/dev/null)
                MEM=$(ps -p $PID -o rss= 2>/dev/null | awk '{printf "%.2f MB", $1/1024}')
                [ -n "$START_TIME" ] && echo "   시작 시간: $START_TIME"
                [ -n "$MEM" ] && echo "   메모리: $MEM"
            else
                START_TIME=$(ps -p $PID -o lstart= 2>/dev/null)
                MEM=$(ps -p $PID -o rss= 2>/dev/null | awk '{printf "%.2f MB", $1/1024}')
                [ -n "$START_TIME" ] && echo "   시작 시간: $START_TIME"
                [ -n "$MEM" ] && echo "   메모리: $MEM"
            fi
        done
    fi
else
    echo "❌ SSH 터널이 실행 중이지 않습니다"
fi

echo ""

# 포트 사용 확인
echo "📡 포트 ${LOCAL_PORT} 사용 현황:"

if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
    # Windows
    PORT_STATUS=$(netstat -ano 2>/dev/null | grep ":${LOCAL_PORT}")
elif [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    PORT_STATUS=$(lsof -i :${LOCAL_PORT} 2>/dev/null)
else
    # Linux
    PORT_STATUS=$(netstat -tuln 2>/dev/null | grep :${LOCAL_PORT})
fi

if [ -n "$PORT_STATUS" ]; then
    echo "$PORT_STATUS" | while IFS= read -r line; do
        echo "   $line"
    done
else
    echo "   포트 ${LOCAL_PORT}이 사용되지 않습니다"
fi
