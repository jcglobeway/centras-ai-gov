-- PostgreSQL only: Enable pgvector and convert embedding_vector to vector type
-- Run this manually on PostgreSQL after V016 migration

-- 1. Enable pgvector extension (if not already)
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Convert embedding_vector from TEXT to vector(1024)
ALTER TABLE document_chunks
ALTER COLUMN embedding_vector TYPE vector(1024) USING NULL;

-- 3. Create vector similarity search index
CREATE INDEX idx_document_chunks_embedding
ON document_chunks USING ivfflat (embedding_vector vector_cosine_ops)
WITH (lists = 100);

-- Verify
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'document_chunks'
AND column_name = 'embedding_vector';
