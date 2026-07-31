-- 기존 배달팟은 생성 당시 방장 정보를 party_participants에 저장하지 않았다.
-- 생성자마다 HOST/JOINED 참여자 행을 보정해 참여자 목록과 인원 수를 일관되게 조회한다.
-- 이미 생성자 행이 MEMBER 또는 CANCELED 상태로 존재하면 방장은 참여 취소할 수 없다는
-- 도메인 규칙에 맞춰 HOST/JOINED로 정정한다.
UPDATE party_participants pp
JOIN delivery_parties dp ON dp.id = pp.party_id
SET pp.role = 'HOST',
    pp.status = 'JOINED',
    pp.canceled_at = NULL
WHERE pp.user_id = dp.creator_id
  AND (pp.role <> 'HOST' OR pp.status <> 'JOINED');

-- 생성자 행이 전혀 없는 배달팟에만 새 HOST/JOINED 참여자 행을 추가한다.
INSERT INTO party_participants (
    party_id,
    user_id,
    role,
    joined_at,
    status,
    version
)
SELECT
    dp.id,
    dp.creator_id,
    'HOST',
    dp.created_at,
    'JOINED',
    0
FROM delivery_parties dp
WHERE NOT EXISTS (
    SELECT 1
    FROM party_participants pp
    WHERE pp.party_id = dp.id
      AND pp.user_id = dp.creator_id
);
