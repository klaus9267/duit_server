-- 마케팅(광고성 정보) 수신 동의를 opt-in 으로 전환
--
-- 배경:
--   AlarmSettings.marketing 기본값이 true 여서 가입만 해도 수신 동의로 저장됐다.
--   정보통신망법 제50조는 사전 명시적 동의를 요구하므로 이 값은 유효한 동의가 아니다.
--
-- 조치:
--   1) 기존 회원 전원의 marketing 을 미동의(false)로 초기화한다. 동의가 필요하면 앱에서 다시 받는다.
--   2) 동의 시각을 기록할 marketing_agreed_at 컬럼을 추가한다 (미동의면 NULL).
UPDATE users
SET marketing = FALSE;

ALTER TABLE users
    ADD COLUMN marketing_agreed_at DATETIME(6) NULL COMMENT '마케팅 수신 동의 시각';
