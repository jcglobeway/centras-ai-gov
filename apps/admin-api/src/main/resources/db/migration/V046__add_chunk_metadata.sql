-- document_chunks에 KG 메타데이터 저장용 JSONB 컬럼 추가
-- H2 호환: JSONB 대신 JSON 타입 사용 (H2는 JSONB 미지원)
ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS metadata TEXT;
