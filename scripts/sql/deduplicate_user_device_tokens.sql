-- user_device_tokens 중복 토큰 정리 스크립트
-- 정책: 같은 token이 여러 row에 존재하면 updated_at이 가장 최신인 row 1건만 유지
-- 동률이면 id가 가장 큰 row를 유지

-- 1) 정리 전 중복 현황 확인
SELECT token, COUNT(*) AS cnt
FROM user_device_tokens
GROUP BY token
HAVING COUNT(*) > 1
ORDER BY cnt DESC, token;

-- 2) 중복 row 삭제
DELETE udt
FROM user_device_tokens udt
JOIN (
    SELECT id
    FROM (
        SELECT
            id,
            ROW_NUMBER() OVER (
                PARTITION BY token
                ORDER BY updated_at DESC, id DESC
            ) AS rn
        FROM user_device_tokens
    ) ranked
    WHERE ranked.rn > 1
) duplicated ON duplicated.id = udt.id;

-- 3) 정리 후 중복이 남아있는지 재확인
SELECT token, COUNT(*) AS cnt
FROM user_device_tokens
GROUP BY token
HAVING COUNT(*) > 1
ORDER BY cnt DESC, token;
