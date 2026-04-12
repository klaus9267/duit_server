-- 멀티 디바이스 토큰 전환용 수동 마이그레이션
-- 실행 순서:
-- 1) 새 앱 배포 전에 CREATE TABLE + 백필까지 수행
-- 2) 새 앱 배포 및 정상 동작 확인
-- 3) 구버전 users.device_token 컬럼 제거

CREATE TABLE IF NOT EXISTS user_device_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_user_device_tokens_token UNIQUE (token),
    CONSTRAINT fk_user_device_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_user_device_tokens_user_id ON user_device_tokens (user_id);

INSERT INTO user_device_tokens (user_id, token, created_at, updated_at)
SELECT u.id, u.device_token, NOW(6), NOW(6)
FROM users u
WHERE u.device_token IS NOT NULL
  AND u.device_token <> ''
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    updated_at = VALUES(updated_at);

-- 새 앱 배포 후 정상 동작을 확인한 뒤 마지막에 실행
-- ALTER TABLE users DROP COLUMN device_token;
