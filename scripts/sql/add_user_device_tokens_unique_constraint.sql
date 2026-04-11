-- user_device_tokens.token 유니크 제약 보강 스크립트
-- 선행 조건: deduplicate_user_device_tokens.sql 실행 완료

SET @schema_name = DATABASE();

SET @unique_constraint_exists = (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE table_schema = @schema_name
      AND table_name = 'user_device_tokens'
      AND constraint_name = 'uk_user_device_tokens_token'
      AND constraint_type = 'UNIQUE'
);

SET @add_unique_constraint_sql = IF(
    @unique_constraint_exists = 0,
    'ALTER TABLE user_device_tokens ADD CONSTRAINT uk_user_device_tokens_token UNIQUE (token)',
    'SELECT ''uk_user_device_tokens_token already exists'' AS message'
);

PREPARE stmt FROM @add_unique_constraint_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
