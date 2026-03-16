-- pgvector 확장 활성화 및 embedding_vector 컬럼 타입을 vector(1024)로 변환
-- 이 마이그레이션은 PostgreSQL 환경에서만 실행 (H2 테스트는 flyway.target=17로 skip)

CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE document_chunks
    ALTER COLUMN embedding_vector TYPE vector(1024) USING NULL;

CREATE INDEX idx_document_chunks_embedding
    ON document_chunks USING ivfflat (embedding_vector vector_cosine_ops)
    WITH (lists = 100);
