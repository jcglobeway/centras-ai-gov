-- Document Chunks Table with pgvector
-- Note: vector type은 PostgreSQL + pgvector에서만 작동 (H2 테스트에서는 TEXT로 저장)
CREATE TABLE document_chunks (
    id VARCHAR(255) PRIMARY KEY,
    document_id VARCHAR(255) NOT NULL,
    document_version_id VARCHAR(255),
    chunk_key VARCHAR(255) NOT NULL,
    chunk_text TEXT NOT NULL,
    chunk_order INTEGER NOT NULL,
    token_count INTEGER,
    embedding_vector TEXT,  -- H2 호환: TEXT로 저장, PostgreSQL에서는 ALTER로 vector로 변경
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES documents(id)
);

CREATE INDEX idx_document_chunks_document_id ON document_chunks(document_id);
CREATE INDEX idx_document_chunks_chunk_order ON document_chunks(chunk_order);

-- Seed development chunks
INSERT INTO document_chunks (id, document_id, chunk_key, chunk_text, chunk_order, token_count, created_at) VALUES
('chunk_001', 'doc_301', 'chunk_0', '서울시 복지 혜택 신청 안내: 온라인 또는 주민센터를 방문하여 신청할 수 있습니다. 필요 서류는 신분증과 소득 증빙 서류입니다. 처리 기간은 신청 후 7-14일입니다.', 0, 50, '2026-03-01 00:00:00'),
('chunk_002', 'doc_302', 'chunk_0', '부산시 자주 묻는 질문: 운영 시간은 평일 09:00-18:00입니다. 주말 및 공휴일은 휴무입니다.', 0, 30, '2026-02-15 00:00:00');
