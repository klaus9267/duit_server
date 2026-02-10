#!/bin/bash

set -e

# Configuration
NGINX_CONFIG="/etc/nginx/nginx.conf"

# Logging functions (GitHub Actions compatible)
log_info() {
    if [[ "${GITHUB_ACTIONS}" == "true" ]]; then
        echo "::notice::$1"
    else
        echo "ℹ️  $1"
    fi
}

log_success() {
    if [[ "${GITHUB_ACTIONS}" == "true" ]]; then
        echo "::notice title=Success::✅ $1"
    else
        echo "✅ $1"
    fi
}

log_error() {
    if [[ "${GITHUB_ACTIONS}" == "true" ]]; then
        echo "::error::❌ $1"
    else
        echo "❌ $1"
    fi
}

# Main function
main() {
    local target_env=$1

    # 입력 검증
    if [[ "$target_env" != "blue" && "$target_env" != "green" ]]; then
        log_error "Usage: $0 <blue|green>"
        exit 1
    fi

    # 가중치 설정
    if [[ "$target_env" == "blue" ]]; then
        sudo sed -i "s|server 127.0.0.1:8082|server 127.0.0.1:8080|g" "$NGINX_CONFIG"
    else
        sudo sed -i "s|server 127.0.0.1:8080|server 127.0.0.1:8082|g" "$NGINX_CONFIG"
    fi

    # Nginx 테스트
    if sudo nginx -t 2>/dev/null; then
        log_success "Nginx configuration test passed"

        # Nginx 리로드
        if sudo nginx -s reload; then
            log_success "Traffic successfully switched to $target_env environment!"
        else
            log_error "Failed to reload Nginx"
            exit 1
        fi
    else
        log_error "Nginx configuration test failed"
        exit 1
    fi
}

# 스크립트 실행
main "$@"
