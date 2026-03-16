-- RAG Search Logs Table
CREATE TABLE rag_search_logs (
    id VARCHAR(255) PRIMARY KEY,
    question_id VARCHAR(255) NOT NULL,
    query_text TEXT NOT NULL,
    query_rewrite_text TEXT,
    zero_result BOOLEAN NOT NULL DEFAULT false,
    top_k INTEGER,
    latency_ms INTEGER,
    retrieval_engine VARCHAR(50),
    retrieval_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (question_id) REFERENCES questions(id)
);

CREATE INDEX idx_rag_search_logs_question_id ON rag_search_logs(question_id);
CREATE INDEX idx_rag_search_logs_retrieval_status ON rag_search_logs(retrieval_status);
CREATE INDEX idx_rag_search_logs_created_at ON rag_search_logs(created_at);
CREATE INDEX idx_rag_search_logs_zero_result ON rag_search_logs(zero_result);

-- RAG Retrieved Documents Table
CREATE TABLE rag_retrieved_documents (
    id VARCHAR(255) PRIMARY KEY,
    rag_search_log_id VARCHAR(255) NOT NULL,
    document_id VARCHAR(255),
    chunk_id VARCHAR(255),
    rank INTEGER NOT NULL,
    score DECIMAL(10,6),
    used_in_citation BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (rag_search_log_id) REFERENCES rag_search_logs(id),
    FOREIGN KEY (document_id) REFERENCES documents(id),
    FOREIGN KEY (chunk_id) REFERENCES document_chunks(id)
);

CREATE INDEX idx_rag_retrieved_docs_search_log_id ON rag_retrieved_documents(rag_search_log_id);
CREATE INDEX idx_rag_retrieved_docs_document_id ON rag_retrieved_documents(document_id);
CREATE INDEX idx_rag_retrieved_docs_chunk_id ON rag_retrieved_documents(chunk_id);
CREATE INDEX idx_rag_retrieved_docs_rank ON rag_retrieved_documents(rank);
