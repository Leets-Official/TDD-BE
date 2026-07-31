-- 배달팟 위치는 방장 회원의 기숙사 인증 정보와 별도로 저장한다.
ALTER TABLE delivery_parties
    ADD COLUMN dormitory VARCHAR(20) NULL;

-- 기존 배달팟은 생성 당시 방장 기숙사를 위치로 사용했으므로, 기존 방장 기숙사 값으로 보정한다.
UPDATE delivery_parties dp
SET dormitory = (
    SELECT dorm.dormitory
    FROM dormitory dorm
    WHERE dorm.user_id = dp.creator_id
)
WHERE dormitory IS NULL;
