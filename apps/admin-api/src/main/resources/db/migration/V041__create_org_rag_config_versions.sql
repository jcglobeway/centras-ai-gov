CREATE TABLE org_rag_config_versions (
    id VARCHAR(50) PRIMARY KEY,
    organization_id VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL,
    system_prompt TEXT NOT NULL,
    tone VARCHAR(20) NOT NULL,
    top_k INTEGER NOT NULL,
    similarity_threshold NUMERIC(4,3) NOT NULL,
    reranker_enabled BOOLEAN NOT NULL,
    llm_model VARCHAR(100) NOT NULL,
    llm_temperature NUMERIC(3,2) NOT NULL,
    llm_max_tokens INTEGER NOT NULL,
    change_note VARCHAR(500),
    changed_by VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_org_rag_config_version UNIQUE (organization_id, version)
);

CREATE INDEX idx_org_rag_config_versions_org ON org_rag_config_versions (organization_id);
CREATE INDEX idx_org_rag_config_versions_org_ver ON org_rag_config_versions (organization_id, version);
