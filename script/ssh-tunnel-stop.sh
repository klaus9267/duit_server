#!/bin/bash
# SSH 터널 종료 스크립트 (Windows Git Bash/macOS/Linux)

# 스크립트가 script/ 폴더에 있으므로 프로젝트 루트 계산
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/docker/.env"

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
LOCAL_PORT="${SSH_LOCAL_PORT}"
SERVER_PORT="${SSH_SERVER_PORT}"

echo "🔌 SSH 터널을 종료합니다..."

# 포트가 사용 중인지 확인
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
    # Windows: netstat으로 포트 확인 및 PID 추출
    PORT_INFO=$(netstat -ano 2>/dev/null | grep ":${LOCAL_PORT}" | grep "LISTENING")
    if [ -n "$PORT_INFO" ]; then
        # Windows PID 추출
        WIN_PIDS=$(echo "$PORT_INFO" | awk '{print $NF}' | sort -u)
        COUNT=$(echo "$WIN_PIDS" | wc -l | xargs)
        echo "   찾은 SSH 터널 프로세스: $COUNT 개"

        for WIN_PID in $WIN_PIDS; do
            echo "   종료 중: PID $WIN_PID (Windows)"
            taskkill //F //PID $WIN_PID 2>/dev/null
        done

        # Git Bash SSH 프로세스도 종료
        BASH_PIDS=$(ps aux | grep "/usr/bin/ssh" | grep -v grep | awk '{print $1}')
        for BASH_PID in $BASH_PIDS; do
            echo "   종료 중: PID $BASH_PID (Git Bash)"
            kill -9 $BASH_PID 2>/dev/null
        done

        echo "✅ SSH 터널이 종료되었습니다"
    else
        echo "⚠️  실행 중인 SSH 터널을 찾을 수 없습니다"
    fi
else
    # macOS/Linux: lsof 또는 netstat로 확인
    if [[ "$OSTYPE" == "darwin"* ]]; then
        PORT_INFO=$(lsof -i :${LOCAL_PORT} 2>/dev/null | grep "LISTEN")
    else
        PORT_INFO=$(netstat -tuln 2>/dev/null | grep ":${LOCAL_PORT}")
    fi

    if [ -n "$PORT_INFO" ]; then
        # SSH 프로세스 찾기
        PIDS=$(pgrep -f "ssh -p ${SERVER_PORT}" 2>/dev/null)
        if [ -n "$PIDS" ]; then
            COUNT=$(echo "$PIDS" | wc -l | xargs)
            echo "   찾은 SSH 터널 프로세스: $COUNT 개"

            for PID in $PIDS; do
                echo "   종료 중: PID $PID"
                kill $PID
            done

            echo "✅ SSH 터널이 종료되었습니다"
        else
            echo "⚠️  포트는 사용 중이지만 SSH 프로세스를 찾을 수 없습니다"
        fi
    else
        echo "⚠️  실행 중인 SSH 터널을 찾을 수 없습니다"
    fi
fi
