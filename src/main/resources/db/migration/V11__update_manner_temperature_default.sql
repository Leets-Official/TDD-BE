-- 매너온도 스케일 변경: 0~100(기본값 36.5) -> 0.0~10.0(기본값 3.0).
-- 범위(0.0~10.0)는 애플리케이션(User.updateMannerTemperature)에서 clamp하므로 DB에는
-- CHECK 제약을 따로 걸지 않는다. 컬럼 default만 새 스케일 기준으로 맞춘다.
ALTER TABLE users MODIFY COLUMN manner_temperature DECIMAL(4, 1) NOT NULL DEFAULT 3.0;

-- 기존에 이미 저장된 row(구 스케일 값, 예: 36.5)는 새 스케일 기준 범위(0.0~10.0)를 벗어나므로
-- 기본값으로 초기화한다.
UPDATE users SET manner_temperature = 3.0;
