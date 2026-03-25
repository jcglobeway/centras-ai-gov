-- Organizations Table
CREATE TABLE organizations (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    org_code VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    institution_type VARCHAR(50) NOT NULL,
    owner_user_id VARCHAR(255),
    last_document_sync_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_user_id) REFERENCES admin_users(id)
);

CREATE INDEX idx_organizations_status ON organizations(status);
CREATE INDEX idx_organizations_org_code ON organizations(org_code);
CREATE INDEX idx_organizations_owner_user_id ON organizations(owner_user_id);

-- Seed development organizations
INSERT INTO organizations (id, name, org_code, status, institution_type, created_at) VALUES
('org_acc',         '국립아시아문화전당',  'ACC_NATIONAL',   'active', 'national_institution', '2026-03-01 00:00:00'),
('org_central_gov', '중앙행정기관',       'CENTRAL_GOV',    'active', 'central_government',   '2026-03-01 00:00:00'),
('org_local_gov',   '지방행정기관',       'LOCAL_GOV',      'active', 'local_government',     '2026-03-01 00:00:00');
