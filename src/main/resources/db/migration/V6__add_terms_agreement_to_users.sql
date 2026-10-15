-- 이용약관 동의 기록
-- 소셜 로그인 시 클라이언트가 보낸 약관 버전(agreedTermsVersion)과 동의 시각을 남긴다.
-- 버전이 바뀔 때만 갱신하므로 terms_agreed_at 은 해당 버전에 처음 동의한 시각이다.
ALTER TABLE users
    ADD COLUMN terms_version   VARCHAR(20) NULL COMMENT '동의한 이용약관 버전',
    ADD COLUMN terms_agreed_at DATETIME(6) NULL COMMENT '이용약관 동의 시각';
