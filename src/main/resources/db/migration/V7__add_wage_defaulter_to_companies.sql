-- 임금체불 명단공개 사업주 표시 (직업안정법 제25조 제1호)
--
-- 고용24 임금체불 명단공개 사업주 API(사업자등록번호 기준) 조회 결과를 저장한다.
--   wage_defaulter            : TRUE = 명단공개 대상, FALSE = 확인 결과 미대상, NULL = 미확인
--   wage_defaulter_checked_at : 마지막으로 결과를 확인한 시각 (조회 실패 시에는 갱신하지 않음)
ALTER TABLE companies
    ADD COLUMN wage_defaulter            BOOLEAN     NULL COMMENT '임금체불 명단공개 사업주 여부',
    ADD COLUMN wage_defaulter_checked_at DATETIME(6) NULL COMMENT '임금체불 명단 확인 시각';
