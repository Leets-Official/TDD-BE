-- ERD 반영: 인증코드 확인 실패 횟수 추적용 attempt_count 컬럼 추가.
-- 5분(코드 TTL) 내 3회 실패 시 이후 확인 시도를 막는 데 사용한다.

ALTER TABLE email_verification_codes
    ADD COLUMN attempt_count INT NOT NULL DEFAULT 0;
