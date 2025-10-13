#!/bin/bash
# SSH 터널 시작 스크립트 (Windows Git Bash/macOS/Linux)
# MySQL 포트 포워딩: localhost:3308 -> 서버 localhost:3306

# 스크립트가 script/ 폴더에 있으므로 프로젝트 루트 계산
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/docker/.env"

# .env 파일 로드
if [ -f "$ENV_FILE" ]; then
    set -a  # 모든 변수를 자동으로 export
    source "$ENV_FILE"
    set +a
else
    echo "❌ .env 파일을 찾을 수 없습니다: $ENV_FILE"
    exit 1
fi

# 환경변수에서 설정 읽기
SERVER_IP="${SSH_SERVER_IP}"
SERVER_USER="${SSH_SERVER_USER}"
SERVER_PORT="${SSH_SERVER_PORT}"
LOCAL_PORT="${SSH_LOCAL_PORT}"
REMOTE_HOST="${SSH_REMOTE_HOST}"
REMOTE_PORT="${SSH_REMOTE_PORT}"

echo "🔌 SSH 터널을 시작합니다..."

# OS 감지
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
    # Windows Git Bash
    EXISTING_PID=$(ps aux | grep "ssh -p ${SERVER_PORT} -fN -L ${LOCAL_PORT}:${REMOTE_HOST}:${REMOTE_PORT}" | grep -v grep | awk '{print $1}')
else
    # macOS/Linux
    EXISTING_PID=$(pgrep -f "ssh -p ${SERVER_PORT} -fN -L ${LOCAL_PORT}:${REMOTE_HOST}:${REMOTE_PORT}" 2>/dev/null)
fi

# 기존 SSH 터널 확인
if [ -n "$EXISTING_PID" ]; then
    echo "⚠️  이미 SSH 터널이 실행 중입니다 (PID: $EXISTING_PID)"
    exit 0
fi

# 먼저 포트가 사용 가능한지 확인
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
    PORT_IN_USE=$(netstat -ano 2>/dev/null | grep ":${LOCAL_PORT}" | grep "LISTENING")
else
    PORT_IN_USE=$(lsof -i :${LOCAL_PORT} 2>/dev/null)
fi

if [ -n "$PORT_IN_USE" ]; then
    echo "❌ SSH 터널 연결 실패: 포트 ${LOCAL_PORT}이(가) 이미 사용 중입니다"
    echo "   ./ssh-tunnel-stop.sh 실행 후 다시 시도하세요"
    echo ""
    echo "포트 사용 현황:"
    echo "$PORT_IN_USE"
    exit 1
fi

# SSH 터널 생성 (백그라운드로 실행)
ssh -p ${SERVER_PORT} -fN -L ${LOCAL_PORT}:${REMOTE_HOST}:${REMOTE_PORT} ${SERVER_USER}@${SERVER_IP}

# 터널이 정상적으로 생성되었는지 확인 (3초 대기)
sleep 3

# 포트가 리스닝 중인지 확인 (더 신뢰할 수 있는 방법)
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
    # Windows: netstat으로 포트 확인
    PORT_LISTENING=$(netstat -ano 2>/dev/null | grep ":${LOCAL_PORT}" | grep "LISTENING")
else
    # macOS/Linux: lsof로 포트 확인
    PORT_LISTENING=$(lsof -i :${LOCAL_PORT} 2>/dev/null | grep "LISTEN")
fi

if [ -n "$PORT_LISTENING" ]; then
    echo "✅ SSH 터널 연결 성공!"
    echo "   localhost:${LOCAL_PORT} → ${SERVER_IP} (${REMOTE_HOST}:${REMOTE_PORT})"
    echo ""
    echo "💡 종료하려면: ./ssh-tunnel-stop.sh"
else
    echo "❌ SSH 터널 연결 실패"
    echo "   다음을 확인하세요:"
    echo "   1. SSH 키 인증이 설정되어 있는지 확인"
    echo "   2. 서버가 접근 가능한지 확인: ssh -p ${SERVER_PORT} ${SERVER_USER}@${SERVER_IP}"
    echo "   3. 방화벽이 포트 ${LOCAL_PORT}을(를) 차단하지 않는지 확인"
    exit 1
fi
