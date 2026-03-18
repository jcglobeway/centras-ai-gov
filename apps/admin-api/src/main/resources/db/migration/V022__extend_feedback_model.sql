-- feedbacks 테이블: 행동 신호 기반 해결율·만족도 측정 컬럼 추가
ALTER TABLE feedbacks ADD COLUMN feedback_type VARCHAR(30);
ALTER TABLE feedbacks ADD COLUMN clicked_link BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE feedbacks ADD COLUMN clicked_document BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE feedbacks ADD COLUMN target_action_type VARCHAR(30);
ALTER TABLE feedbacks ADD COLUMN target_action_completed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE feedbacks ADD COLUMN dwell_time_ms BIGINT;
