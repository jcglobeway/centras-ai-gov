-- Document Versions Table
CREATE TABLE document_versions (
    id VARCHAR(255) PRIMARY KEY,
    document_id VARCHAR(255) NOT NULL,
    version_label VARCHAR(100) NOT NULL,
    content_hash VARCHAR(255),
    source_etag VARCHAR(255),
    source_last_modified_at TIMESTAMP,
    change_detected BOOLEAN NOT NULL DEFAULT false,
    snapshot_uri TEXT,
    parsed_text_uri TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES documents(id)
);

CREATE INDEX idx_document_versions_document_id ON document_versions(document_id);
CREATE INDEX idx_document_versions_created_at ON document_versions(created_at);

-- Seed development document versions
INSERT INTO document_versions (id, document_id, version_label, content_hash, change_detected, created_at) VALUES
('doc_ver_001', 'doc_301', 'v1.0', 'hash_welfare_guide_v1', false, '2026-03-01 00:00:00'),
('doc_ver_002', 'doc_302', 'v1.2', 'hash_faq_v12', false, '2026-02-15 00:00:00');
