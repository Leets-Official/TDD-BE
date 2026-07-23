-- 로그인 5분 내 3회 실패 시 15분간 차단하기 위한 컬럼.
-- last_failed_login_at 하나로 "5분 리셋 판단"과 "15분 차단 판단"을 둘 다 계산한다.
-- (실패 횟수 3회 미만일 때: 이 시각으로부터 5분이 지나면 카운트를 리셋
--  실패 횟수 3회 이상일 때: 이 시각으로부터 15분이 지나기 전까지는 차단)
ALTER TABLE users ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN last_failed_login_at DATETIME(6) NULL;
