-- Admin Sessions Table
CREATE TABLE admin_sessions (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    session_token_hash VARCHAR(255) NOT NULL,
    snapshot_json TEXT NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    last_seen_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    ip_address VARCHAR(255),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES admin_users(id)
);

CREATE INDEX idx_admin_sessions_user_id ON admin_sessions(user_id);
CREATE INDEX idx_admin_sessions_token_hash ON admin_sessions(session_token_hash);
CREATE INDEX idx_admin_sessions_expires_at ON admin_sessions(expires_at);
CREATE INDEX idx_admin_sessions_revoked_at ON admin_sessions(revoked_at);

-- Seed development sessions
INSERT INTO admin_sessions (id, user_id, session_token_hash, snapshot_json, issued_at, expires_at, last_seen_at, created_at) VALUES
('sess_ops_global_001', 'usr_ops_global_001', 'hash_ops_001',
'{"user":{"id":"usr_ops_global_001","email":"ops.platform@gov-platform.kr","displayName":"Platform Operator","status":"ACTIVE","lastLoginAt":"2026-03-15T08:30:00Z"},"roleAssignments":[{"roleCode":"ops_admin","organizationId":null}],"grantedActions":["dashboard.read","organization.read","crawl_source.read","crawl_source.write"]}',
'2026-03-15 08:00:00', '2026-03-15 18:00:00', '2026-03-15 08:00:00', '2026-03-15 08:00:00'),
('sess_client_busan_001', 'usr_client_busan_001', 'hash_client_001',
'{"user":{"id":"usr_client_busan_001","email":"client.admin@busan.go.kr","displayName":"Busan Client Admin","status":"ACTIVE","lastLoginAt":"2026-03-15T08:30:00Z"},"roleAssignments":[{"roleCode":"client_admin","organizationId":"org_busan_220"}],"grantedActions":["dashboard.read","crawl_source.read"]}',
'2026-03-15 08:15:00', '2026-03-15 18:15:00', '2026-03-15 08:15:00', '2026-03-15 08:15:00');

-- Expired session for testing
INSERT INTO admin_sessions (id, user_id, session_token_hash, snapshot_json, issued_at, expires_at, last_seen_at, created_at) VALUES
('sess_expired_qa_001', 'usr_qa_001', 'hash_qa_001',
'{"user":{"id":"usr_qa_001","email":"qa.manager@gov-platform.kr","displayName":"QA Manager","status":"ACTIVE","lastLoginAt":"2026-03-15T08:30:00Z"},"roleAssignments":[{"roleCode":"qa_admin","organizationId":"org_seoul_120"}],"grantedActions":["dashboard.read","qa.review.read"]}',
'2026-03-14 00:00:00', '2026-03-14 08:00:00', '2026-03-14 00:00:00', '2026-03-14 00:00:00');
