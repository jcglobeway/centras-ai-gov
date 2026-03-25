-- Chat Sessions Table
CREATE TABLE chat_sessions (
    id VARCHAR(255) PRIMARY KEY,
    organization_id VARCHAR(255) NOT NULL,
    service_id VARCHAR(255) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    user_key_hash VARCHAR(255),
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    FOREIGN KEY (service_id) REFERENCES services(id)
);

CREATE INDEX idx_chat_sessions_organization_id ON chat_sessions(organization_id);
CREATE INDEX idx_chat_sessions_service_id ON chat_sessions(service_id);
CREATE INDEX idx_chat_sessions_started_at ON chat_sessions(started_at);

-- Seed development chat sessions
INSERT INTO chat_sessions (id, organization_id, service_id, channel, user_key_hash, started_at, created_at) VALUES
('chat_session_001', 'org_local_gov',   'svc_welfare',     'web', 'user_hash_001', '2026-03-15 09:00:00', '2026-03-15 09:00:00'),
('chat_session_002', 'org_central_gov', 'svc_faq',         'web', 'user_hash_002', '2026-03-15 09:30:00', '2026-03-15 09:30:00'),
('chat_session_003', 'org_acc',         'svc_acc_chatbot', 'web', 'user_hash_003', '2026-03-15 10:00:00', '2026-03-15 10:00:00');
