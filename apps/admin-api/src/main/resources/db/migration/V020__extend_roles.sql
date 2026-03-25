-- 신규 역할 사용자 추가: super_admin, client_viewer, knowledge_editor
INSERT INTO admin_users (id, email, display_name, status, last_login_at, created_at) VALUES
('usr_super_001',             'super.admin@gov-platform.kr',      'Super Admin',      'active', '2026-03-15 08:30:00', '2026-03-01 00:00:00'),
('usr_client_viewer_001',     'client.viewer@acc.go.kr',          '조회 전용 사용자', 'active', '2026-03-15 08:30:00', '2026-03-01 00:00:00'),
('usr_knowledge_editor_001',  'knowledge.editor@gov-platform.kr', '문서 편집자',      'active', '2026-03-15 08:30:00', '2026-03-01 00:00:00');

INSERT INTO admin_user_roles (id, user_id, role_code, organization_id, assigned_at) VALUES
('role_005', 'usr_super_001',            'super_admin',     NULL,            '2026-03-01 00:00:00'),
('role_006', 'usr_client_viewer_001',    'client_viewer',   'org_acc',       '2026-03-01 00:00:00'),
('role_007', 'usr_knowledge_editor_001', 'knowledge_editor','org_local_gov', '2026-03-01 00:00:00');
