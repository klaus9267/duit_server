-- 구독 알림 도입에 따른 alarms 테이블 확장
--
-- 1) 알람이 event 또는 job_posting 둘 중 하나만 참조 가능하도록 event_id 를 NULL 허용
ALTER TABLE alarms MODIFY COLUMN event_id BIGINT NULL;

-- 2) 채용공고 알림용 FK 추가
ALTER TABLE alarms
    ADD COLUMN job_posting_id BIGINT NULL,
    ADD CONSTRAINT fk_alarm_job_posting FOREIGN KEY (job_posting_id) REFERENCES job_postings (id);

-- NOTE: 기존 UNIQUE (user_id, event_id, type) 는 유지.
--   - 이벤트 알람 dedup 에는 여전히 유효
--   - 구독 알람(event_id IS NULL) 은 MySQL UNIQUE 의 NULL distinct 특성으로 DB 단 dedup 안 됨
--     → AlarmService / SubscriptionNotificationService 에서 existsBy... 명시 체크로 처리 (기존 이벤트 알람과 동일 패턴)
--   - user_id FK 의 backing 인덱스 역할도 하므로 함부로 떼면 안 됨
