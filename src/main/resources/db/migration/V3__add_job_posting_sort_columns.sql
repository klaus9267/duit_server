ALTER TABLE job_postings
    ADD COLUMN expires_at DATETIME(6) NULL COMMENT '정렬용 마감 시각',
    ADD COLUMN salary_min BIGINT NULL COMMENT '정렬용 최소 급여(원)';

UPDATE job_postings
SET expires_at = CASE
    WHEN TRIM(receipt_close_dt) REGEXP '^[0-9]{8}$' THEN STR_TO_DATE(TRIM(receipt_close_dt), '%Y%m%d')
    WHEN TRIM(receipt_close_dt) REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}$' THEN STR_TO_DATE(TRIM(receipt_close_dt), '%Y-%m-%d')
    WHEN TRIM(receipt_close_dt) REGEXP '^[0-9]{2}-[0-9]{2}-[0-9]{2}$' THEN STR_TO_DATE(TRIM(receipt_close_dt), '%y-%m-%d')
    ELSE NULL
END;

UPDATE job_postings
SET salary_min = CAST(REPLACE(REGEXP_SUBSTR(sal_tp_nm, '[0-9][0-9,]*'), ',', '') AS UNSIGNED)
WHERE sal_tp_nm REGEXP '[0-9]';

CREATE INDEX idx_job_postings_active_created
    ON job_postings (is_active, created_at DESC, id DESC);
CREATE INDEX idx_job_postings_active_expires
    ON job_postings (is_active, expires_at ASC, id DESC);
CREATE INDEX idx_job_postings_active_salary
    ON job_postings (is_active, salary_min DESC, id DESC);
