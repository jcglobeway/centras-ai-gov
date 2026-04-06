-- V029: questions 테이블에 질문 임베딩 컬럼 추가
-- pgvector extension은 V018에서 이미 활성화되어 있으므로 vector(1024) 타입을 직접 사용한다.
ALTER TABLE questions ADD COLUMN question_embedding vector(1024);
