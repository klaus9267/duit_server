ALTER TABLE job_postings
    ADD COLUMN posted_at DATETIME(6) NULL COMMENT '고용24 등록 시각',
    ADD COLUMN expires_at DATETIME(6) NULL COMMENT '정렬용 마감 시각',
    ADD COLUMN salary_min BIGINT NULL COMMENT '정렬용 환산 연간 최소 급여(원)';

UPDATE job_postings
SET posted_at = created_at;

ALTER TABLE job_postings
    MODIFY COLUMN posted_at DATETIME(6) NOT NULL COMMENT '고용24 등록 시각';

UPDATE job_postings
SET expires_at = CASE
    WHEN TRIM(receipt_close_dt) REGEXP '^[0-9]{8}$' THEN STR_TO_DATE(TRIM(receipt_close_dt), '%Y%m%d')
    WHEN TRIM(receipt_close_dt) REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}$' THEN STR_TO_DATE(TRIM(receipt_close_dt), '%Y-%m-%d')
    WHEN TRIM(receipt_close_dt) REGEXP '^[0-9]{2}-[0-9]{2}-[0-9]{2}$' THEN STR_TO_DATE(TRIM(receipt_close_dt), '%y-%m-%d')
    ELSE NULL
END;

UPDATE job_postings
SET is_active = FALSE
WHERE expires_at < CURRENT_DATE();

WITH salary_segments AS (
    SELECT id,
           CASE
               WHEN REGEXP_INSTR(sal_tp_nm, '연봉|월급|시급|일급|급여') > 0
                   THEN SUBSTRING(sal_tp_nm, REGEXP_INSTR(sal_tp_nm, '연봉|월급|시급|일급|급여'))
               ELSE sal_tp_nm
           END AS salary_segment
    FROM job_postings
    WHERE sal_tp_nm REGEXP '[0-9]'
),
salary_parts AS (
    SELECT id,
           salary_segment,
           REGEXP_SUBSTR(salary_segment, '[0-9][0-9,]*') AS amount_text
    FROM salary_segments
),
salary_values AS (
    SELECT id,
           salary_segment,
           REPLACE(amount_text, ',', '') AS amount_digits,
           REGEXP_SUBSTR(
               SUBSTRING(
                   salary_segment,
                   REGEXP_INSTR(salary_segment, '[0-9][0-9,]*') + CHAR_LENGTH(amount_text)
               ),
               '만[[:space:]]*원|원'
           ) AS unit_text
    FROM salary_parts
),
salary_normalized AS (
    SELECT id,
           amount_digits,
           CASE WHEN unit_text REGEXP '^만' THEN 10000 ELSE 1 END *
           CASE
               WHEN salary_segment REGEXP '^월급' THEN 12
               WHEN salary_segment REGEXP '^시급' THEN 2508
               WHEN salary_segment REGEXP '^일급' THEN 261
               ELSE 1
           END AS multiplier
    FROM salary_values
)
UPDATE job_postings jp
JOIN salary_normalized sv ON sv.id = jp.id
SET jp.salary_min = CASE
    WHEN CHAR_LENGTH(sv.amount_digits) > 19 THEN NULL
    WHEN CAST(sv.amount_digits AS DECIMAL(20, 0)) <= 0 THEN NULL
    WHEN CAST(sv.amount_digits AS DECIMAL(20, 0)) > 9223372036854775807 / sv.multiplier THEN NULL
    ELSE CAST(
        CAST(sv.amount_digits AS DECIMAL(20, 0)) * sv.multiplier
        AS SIGNED
    )
END;

CREATE INDEX idx_job_postings_active_created
    ON job_postings (is_active, posted_at DESC, id DESC);
CREATE INDEX idx_job_postings_active_expires
    ON job_postings (is_active, expires_at ASC, id DESC);
CREATE INDEX idx_job_postings_active_salary
    ON job_postings (is_active, salary_min DESC, id DESC);
