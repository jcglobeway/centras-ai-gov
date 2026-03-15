-- Admin Users Table
CREATE TABLE admin_users (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_users_email ON admin_users(email);
CREATE INDEX idx_admin_users_status ON admin_users(status);

-- Admin User Roles Table
CREATE TABLE admin_user_roles (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    organization_id VARCHAR(255),
    service_scope_json TEXT,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES admin_users(id)
);

CREATE INDEX idx_admin_user_roles_user_id ON admin_user_roles(user_id);
CREATE INDEX idx_admin_user_roles_role_code ON admin_user_roles(role_code);
CREATE INDEX idx_admin_user_roles_organization_id ON admin_user_roles(organization_id);

-- Seed development users
INSERT INTO admin_users (id, email, display_name, status, last_login_at, created_at) VALUES
('usr_ops_global_001', 'ops.platform@gov-platform.kr', 'Platform Operator', 'active', '2026-03-15 08:30:00', '2026-03-01 00:00:00'),
('usr_client_busan_001', 'client.admin@busan.go.kr', 'Busan Client Admin', 'active', '2026-03-15 08:30:00', '2026-03-01 00:00:00'),
('usr_qa_001', 'qa.manager@gov-platform.kr', 'QA Manager', 'active', '2026-03-15 08:30:00', '2026-03-01 00:00:00');

INSERT INTO admin_user_roles (id, user_id, role_code, organization_id, assigned_at) VALUES
('role_001', 'usr_ops_global_001', 'ops_admin', NULL, '2026-03-01 00:00:00'),
('role_002', 'usr_client_busan_001', 'client_admin', 'org_busan_220', '2026-03-01 00:00:00'),
('role_003', 'usr_qa_001', 'qa_admin', 'org_seoul_120', '2026-03-01 00:00:00');
