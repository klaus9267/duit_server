-- 행사 다중 주최 지원: events.host_id (N:1) → event_hosts (N:M)
--
-- sort_order: 요청에 들어온 주최 순서. 0 이 첫 번째 주최이며 API 응답의 host(구 클라이언트 호환 필드)로 쓰인다.
--
-- events.host_id 는 이번 릴리스에서 유지한다 (blue-green 배포).
--   새 컨테이너 기동 시 이 마이그레이션이 먼저 적용되고, 트래픽 전환 전·헬스체크 실패 롤백 시에는
--   구 서버가 같은 DB 로 계속 요청을 처리한다. 구 서버는 events.host_id 를 직접 읽고 쓰므로
--   새 서버도 첫 번째 주최 id 를 events.host_id 에 계속 기록한다 (Event.host 미러 필드).
--
-- TODO(event-hosts): 구 서버가 모두 내려간 뒤 V5 에서 아래를 순서대로 수행
--   1) 전환 구간에 구 서버가 기록한 행 백필 (event_hosts 행이 없는 행사)
--        INSERT INTO event_hosts (event_id, host_id, sort_order)
--        SELECT e.id, e.host_id, 0 FROM events e
--        WHERE e.host_id IS NOT NULL
--          AND NOT EXISTS (SELECT 1 FROM event_hosts eh WHERE eh.event_id = e.id);
--   2) events.host_id 의 FK 제거 — Flyway 이전 Hibernate 가 만든 자동 생성 이름이라
--      운영 DB 에서 SHOW CREATE TABLE events 로 FK 이름을 확인한 뒤 DROP FOREIGN KEY
--   3) ALTER TABLE events DROP COLUMN host_id
--   4) Event.host 필드와 hosts 의 legacy fallback 제거
CREATE TABLE event_hosts (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    event_id    BIGINT NOT NULL,
    host_id     BIGINT NOT NULL,
    sort_order  INT    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_event_hosts_event_host UNIQUE (event_id, host_id),
    INDEX idx_event_hosts_host_event (host_id, event_id),
    CONSTRAINT fk_event_host_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    CONSTRAINT fk_event_host_host  FOREIGN KEY (host_id)  REFERENCES hosts (id)
);

INSERT INTO event_hosts (event_id, host_id, sort_order)
SELECT id, host_id, 0
FROM events
WHERE host_id IS NOT NULL;
