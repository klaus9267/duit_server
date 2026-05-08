-- 구독(Subscription) 테이블 생성
-- 한 테이블에 5개 type 의 폴리모픽 row 저장:
--   EVENT_KEYWORD, JOB_KEYWORD : keyword 채움
--   EVENT_HOST                 : host_id 채움
--   EVENT_TYPE                 : event_type 채움
--   JOB_COMPANY                : company_id 채움
-- 사용 안 되는 컬럼은 NULL. type 별 invariant 는 엔티티 init 블록에서 검증.
-- 동일 사용자의 중복 구독 방지는 SubscriptionService 에서 existsBy... 체크로 처리
-- (MySQL UNIQUE 가 NULL distinct 라 nullable 컬럼 조합으로는 안전한 dedup 불가).
CREATE TABLE subscriptions (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    type        VARCHAR(30)  NOT NULL,
    keyword     VARCHAR(50)  NULL,
    host_id     BIGINT       NULL,
    event_type  VARCHAR(30)  NULL,
    company_id  BIGINT       NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_subscription_user    FOREIGN KEY (user_id)    REFERENCES users     (id),
    CONSTRAINT fk_subscription_host    FOREIGN KEY (host_id)    REFERENCES hosts     (id),
    CONSTRAINT fk_subscription_company FOREIGN KEY (company_id) REFERENCES companies (id)
);
