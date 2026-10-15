-- 최저임금 미달 구인정보 게재 금지 (직업안정법 제25조 제2호, 시행령 제28조 제6호)
--
-- 공고 수집(insert/update) 때 임금을 최저임금과 비교해 저장하고, TRUE 인 공고는
-- 목록·상세·북마크·구독 알림에서 모두 제외한다.
-- 기존 공고는 FALSE 로 시작하며, 배포 직후 실행되는 공고 수집에서 활성 공고가 다시 판정된다.
ALTER TABLE job_postings
    ADD COLUMN below_minimum_wage BOOLEAN NOT NULL DEFAULT FALSE COMMENT '최저임금 미달 공고 여부';
