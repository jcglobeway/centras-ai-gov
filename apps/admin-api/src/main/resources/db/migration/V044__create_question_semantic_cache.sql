CREATE TABLE question_semantic_cache (
    id              VARCHAR(255) PRIMARY KEY,
    organization_id VARCHAR(255) NOT NULL,
    question_text   TEXT NOT NULL,
    embedding_vector vector(1024),
    cached_answer   JSONB NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_hit_at     TIMESTAMP,
    hit_count       INT NOT NULL DEFAULT 0
);

CREATE INDEX ON question_semantic_cache
    USING ivfflat (embedding_vector vector_cosine_ops)
    WITH (lists = 10);
