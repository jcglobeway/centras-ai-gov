-- V032: rag_search_logs에 LLM 단계 소요시간 컬럼 추가
-- 기존 latency_ms = retrieval(embedding+pgvector) 단계 소요시간
-- 신규 llm_ms     = LLM 생성 단계 소요시간
-- postprocess_ms는 total - retrieval - llm 으로 API에서 계산
ALTER TABLE rag_search_logs ADD COLUMN llm_ms INTEGER;
