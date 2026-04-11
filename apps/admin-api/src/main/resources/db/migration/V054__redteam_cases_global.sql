-- ── 레드팀 케이스셋 전역화 ────────────────────────────────────────────────────
-- 케이스는 기관별이 아닌 전역 공용 자산으로 관리한다.
-- organization_id 컬럼 제거, 배치 실행 시 대상 기관을 지정하는 방식으로 변경.

-- 기관별 중복 케이스 제거 (org_ggc, org_gjf, org_namgu 전용 케이스)
DELETE FROM redteam_cases
WHERE id LIKE 'rt_ggc_%'
   OR id LIKE 'rt_gjf_%'
   OR id LIKE 'rt_namgu_%';

-- organization_id 컬럼 제거
ALTER TABLE redteam_cases DROP COLUMN organization_id;
