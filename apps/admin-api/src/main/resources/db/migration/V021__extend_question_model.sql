-- questions 테이블: 민원 분류·분석 컬럼 추가
ALTER TABLE questions ADD COLUMN question_category VARCHAR(50);
ALTER TABLE questions ADD COLUMN answer_confidence DECIMAL(5,4);
ALTER TABLE questions ADD COLUMN failure_reason_code VARCHAR(10);
ALTER TABLE questions ADD COLUMN is_escalated BOOLEAN NOT NULL DEFAULT FALSE;

-- chat_sessions 테이블: 세션 종료 유형·턴 수 컬럼 추가
ALTER TABLE chat_sessions ADD COLUMN session_end_type VARCHAR(30);
ALTER TABLE chat_sessions ADD COLUMN total_question_count INTEGER NOT NULL DEFAULT 0;
