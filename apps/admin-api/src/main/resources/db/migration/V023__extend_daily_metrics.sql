-- daily_metrics_org 테이블: KPI 지표 10개 컬럼 추가
ALTER TABLE daily_metrics_org ADD COLUMN auto_resolution_rate DECIMAL(5,4);
ALTER TABLE daily_metrics_org ADD COLUMN escalation_rate DECIMAL(5,4);
ALTER TABLE daily_metrics_org ADD COLUMN explicit_resolution_rate DECIMAL(5,4);
ALTER TABLE daily_metrics_org ADD COLUMN estimated_resolution_rate DECIMAL(5,4);
ALTER TABLE daily_metrics_org ADD COLUMN revisit_rate DECIMAL(5,4);
ALTER TABLE daily_metrics_org ADD COLUMN after_hours_rate DECIMAL(5,4);
ALTER TABLE daily_metrics_org ADD COLUMN avg_session_turn_count DECIMAL(5,2);
ALTER TABLE daily_metrics_org ADD COLUMN knowledge_gap_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE daily_metrics_org ADD COLUMN unanswered_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE daily_metrics_org ADD COLUMN low_satisfaction_count INTEGER NOT NULL DEFAULT 0;
