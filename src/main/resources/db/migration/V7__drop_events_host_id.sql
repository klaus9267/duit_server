-- 행사 다중 주최 3단계: events.host_id 컬럼 삭제 (P-14)
--
-- V6(P-13) 버전부터 앱은 이 컬럼을 매핑하지 않는다. 그 버전이 운영에 배포된 뒤이므로
-- 배포 전환 중이나 롤백 때 이 마이그레이션 이전 버전이 돌아도 컬럼이 없어서 문제 될 일이 없다.
-- 컬럼의 인덱스(Flyway 도입 전 Hibernate 가 만든 FK 인덱스, FK 자체는 V6 에서 삭제)도 함께 사라진다.
ALTER TABLE events DROP COLUMN host_id;
