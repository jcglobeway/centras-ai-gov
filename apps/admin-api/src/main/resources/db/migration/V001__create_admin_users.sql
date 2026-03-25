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
('usr_ops_global_001',    'ops.platform@gov-platform.kr', '운영사 관리자',            'active', '2026-03-15 08:30:00', '2026-03-01 00:00:00'),
('usr_client_acc_001',    'client.admin@acc.go.kr',       '국립아시아문화전당 관리자', 'active', '2026-03-15 08:30:00', '2026-03-01 00:00:00'),
('usr_client_central_001','client.admin@mois.go.kr',      '중앙행정기관 관리자',      'active', '2026-03-15 08:30:00', '2026-03-01 00:00:00'),
('usr_qa_001',            'qa.manager@gov-platform.kr',   'QA 담당자',                'active', '2026-03-15 08:30:00', '2026-03-01 00:00:00');

INSERT INTO admin_user_roles (id, user_id, role_code, organization_id, assigned_at) VALUES
('role_001', 'usr_ops_global_001',    'ops_admin',   NULL,            '2026-03-01 00:00:00'),
('role_002', 'usr_client_acc_001',    'client_admin','org_acc',       '2026-03-01 00:00:00'),
('role_003', 'usr_client_central_001','client_admin','org_central_gov','2026-03-01 00:00:00'),
('role_004', 'usr_qa_001',            'qa_admin',    'org_local_gov', '2026-03-01 00:00:00');
