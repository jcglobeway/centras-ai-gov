-- Services Table
CREATE TABLE services (
    id VARCHAR(255) PRIMARY KEY,
    organization_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    channel_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    go_live_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE INDEX idx_services_organization_id ON services(organization_id);
CREATE INDEX idx_services_status ON services(status);

-- Seed development services
INSERT INTO services (id, organization_id, name, channel_type, status, go_live_at, created_at) VALUES
('svc_acc_chatbot',   'org_acc',         '국립아시아문화전당 안내 챗봇', 'web', 'active', '2026-02-01 00:00:00', '2026-02-01 00:00:00'),
('svc_welfare',       'org_local_gov',   '지방행정기관 민원 챗봇',       'web', 'active', '2026-02-01 00:00:00', '2026-02-01 00:00:00'),
('svc_faq',           'org_central_gov', '중앙행정기관 민원 챗봇',       'web', 'active', '2026-02-15 00:00:00', '2026-02-15 00:00:00');
