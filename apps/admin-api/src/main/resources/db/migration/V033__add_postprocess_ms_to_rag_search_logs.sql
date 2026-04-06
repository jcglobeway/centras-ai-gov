-- V033: rag_search_logs에 후처리 단계 소요시간 컬럼 추가
-- postprocess_ms = LLM 생성 완료 후 신뢰도 계산, 응답 직렬화 등 후처리 단계 소요시간
ALTER TABLE rag_search_logs ADD COLUMN postprocess_ms INTEGER;
