-- 행사 다중 주최 2단계: events.host_id 사용 중단 (P-13)
--
-- 이 버전부터 앱은 events.host_id 를 읽지도 쓰지도 않고 주최는 event_hosts 만 본다.
-- 컬럼은 남긴다: 배포 전환 중과 롤백 시에는 V5 버전 서버가 같은 DB 로 요청을 처리하며 이 컬럼을 매핑한다.
-- 컬럼 삭제는 그다음 배포(P-14, V7)에서 한다.

-- 1. event_hosts 가 없는 행사 백필 (V5 적용 직후 전환 구간에 다중 주최 이전 서버가 만든 행사)
INSERT INTO event_hosts (event_id, host_id, sort_order)
SELECT e.id, e.host_id, 0
FROM events e
WHERE e.host_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM event_hosts eh WHERE eh.event_id = e.id);

-- 2. host_id FK 제거
--    이 버전이 만드는 행사는 host_id 가 NULL 이고 주최를 바꿔도 host_id 를 갱신하지 않는다.
--    FK 가 남아 있으면 행사에서 빠진 주최를 삭제할 때 남은 host_id 참조가 삭제를 막는다.
--    FK 는 Flyway 도입 전 Hibernate 가 자동 이름으로 만들었으므로 이름을 조회해서 지우고, 없으면 건너뛴다.
SET @events_host_fk = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'events'
      AND COLUMN_NAME = 'host_id'
      AND REFERENCED_TABLE_NAME = 'hosts'
);
SET @drop_events_host_fk = IF(
    @events_host_fk IS NULL,
    'DO 0',
    CONCAT('ALTER TABLE events DROP FOREIGN KEY `', @events_host_fk, '`')
);
PREPARE drop_events_host_fk FROM @drop_events_host_fk;
EXECUTE drop_events_host_fk;
DEALLOCATE PREPARE drop_events_host_fk;

-- 3. NULL 허용 (이미 NULL 허용이면 변화 없음)
ALTER TABLE events MODIFY host_id BIGINT NULL;
