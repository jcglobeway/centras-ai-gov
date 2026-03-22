-- V028: V023 고객사 KPI 컬럼에 데모 시드 값 채우기
-- V024 시드의 daily_metrics_org 행에 auto_resolution_rate 등 비즈니스 지표 UPDATE

UPDATE daily_metrics_org SET
    auto_resolution_rate      = 0.6800,
    escalation_rate           = 0.2100,
    explicit_resolution_rate  = 0.4200,
    estimated_resolution_rate = 0.6100,
    revisit_rate              = 0.0820,
    after_hours_rate          = 0.3400,
    avg_session_turn_count    = 2.30,
    knowledge_gap_count       = 5,
    unanswered_count          = 7,
    low_satisfaction_count    = 3
WHERE organization_id = 'org_seoul_120';

UPDATE daily_metrics_org SET
    auto_resolution_rate      = 0.7200,
    escalation_rate           = 0.1600,
    explicit_resolution_rate  = 0.4800,
    estimated_resolution_rate = 0.6700,
    revisit_rate              = 0.0610,
    after_hours_rate          = 0.2900,
    avg_session_turn_count    = 2.10,
    knowledge_gap_count       = 3,
    unanswered_count          = 4,
    low_satisfaction_count    = 2
WHERE organization_id = 'org_busan_220';