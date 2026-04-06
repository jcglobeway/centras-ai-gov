CREATE TABLE org_rag_configs (
    id VARCHAR(50) PRIMARY KEY,
    organization_id VARCHAR(50) NOT NULL,
    system_prompt TEXT NOT NULL,
    tone VARCHAR(20) NOT NULL DEFAULT 'formal',
    top_k INTEGER NOT NULL DEFAULT 10,
    similarity_threshold NUMERIC(4,3) NOT NULL DEFAULT 0.700,
    reranker_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    llm_model VARCHAR(100) NOT NULL DEFAULT 'qwen2.5:7b',
    llm_temperature NUMERIC(3,2) NOT NULL DEFAULT 0.30,
    llm_max_tokens INTEGER NOT NULL DEFAULT 500,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_org_rag_config_org UNIQUE (organization_id)
);

CREATE INDEX idx_org_rag_configs_org ON org_rag_configs (organization_id);
