-- Documents Table
CREATE TABLE documents (
    id VARCHAR(255) PRIMARY KEY,
    organization_id VARCHAR(255) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    source_uri TEXT NOT NULL,
    version_label VARCHAR(100),
    published_at TIMESTAMP,
    ingestion_status VARCHAR(50) NOT NULL,
    index_status VARCHAR(50) NOT NULL,
    visibility_scope VARCHAR(50) NOT NULL,
    last_ingested_at TIMESTAMP,
    last_indexed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE INDEX idx_documents_organization_id ON documents(organization_id);
CREATE INDEX idx_documents_ingestion_status ON documents(ingestion_status);
CREATE INDEX idx_documents_index_status ON documents(index_status);

-- Seed development documents
INSERT INTO documents (id, organization_id, document_type, title, source_uri, version_label, published_at, ingestion_status, index_status, visibility_scope, created_at) VALUES
('doc_301', 'org_acc',         'notice', '국립아시아문화전당 전시 안내', 'https://www.acc.go.kr/guide.pdf',          'v1.0', '2026-03-01 00:00:00', 'completed', 'indexed', 'public', '2026-03-01 00:00:00'),
('doc_302', 'org_central_gov', 'faq',    '중앙행정기관 민원 안내 FAQ',  'https://www.gov.kr/faq.html',              'v1.2', '2026-02-15 00:00:00', 'completed', 'indexed', 'public', '2026-02-15 00:00:00');
