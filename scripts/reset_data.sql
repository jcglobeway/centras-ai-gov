-- E2E 검증용 데이터 리셋 스크립트
-- 실행 대상: PostgreSQL (로컬 개발 환경)
-- 보존: organizations, services, admin_users, crawl_sources, documents, document_versions
-- 삭제: 질문/답변/세션/피드백/QA리뷰/RAGAS평가 (실제 질의 투입 전 초기화)

BEGIN;

DELETE FROM ragas_evaluations;
DELETE FROM feedbacks;
DELETE FROM qa_reviews;
DELETE FROM rag_retrieved_documents;
DELETE FROM rag_search_logs;
DELETE FROM answers;
DELETE FROM questions;
DELETE FROM chat_sessions;

-- daily_metrics_org V023 컬럼 리셋 (실제 집계 전 초기화)
UPDATE daily_metrics_org SET
    total_sessions          = 0,
    total_questions         = 0,
    resolved_rate           = NULL,
    fallback_rate           = NULL,
    zero_result_rate        = NULL,
    avg_response_time_ms    = NULL,
    auto_resolution_rate    = NULL,
    escalation_rate         = NULL,
    explicit_resolution_rate= NULL,
    estimated_resolution_rate = NULL,
    revisit_rate            = NULL,
    after_hours_rate        = NULL,
    avg_session_turn_count  = NULL,
    knowledge_gap_count     = 0,
    unanswered_count        = 0,
    low_satisfaction_count  = 0;

COMMIT;

-- 확인 쿼리
SELECT 'chat_sessions' AS tbl, COUNT(*) FROM chat_sessions
UNION ALL SELECT 'questions',  COUNT(*) FROM questions
UNION ALL SELECT 'answers',    COUNT(*) FROM answers
UNION ALL SELECT 'qa_reviews', COUNT(*) FROM qa_reviews
UNION ALL SELECT 'ragas_evaluations', COUNT(*) FROM ragas_evaluations;
