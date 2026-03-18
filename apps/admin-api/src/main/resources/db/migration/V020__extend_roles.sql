-- 신규 역할 사용자 추가: super_admin, client_viewer, knowledge_editor
INSERT INTO admin_users (id, email, display_name, status, last_login_at, created_at) VALUES
('usr_super_001', 'super.admin@gov-platform.kr', 'Super Admin', 'active', '2026-03-15 08:30:00', '2026-03-01 00:00:00'),
('usr_client_viewer_001', 'client.viewer@busan.go.kr', 'Busan Client Viewer', 'active', '2026-03-15 08:30:00', '2026-03-01 00:00:00'),
('usr_knowledge_editor_001', 'knowledge.editor@gov-platform.kr', 'Knowledge Editor', 'active', '2026-03-15 08:30:00', '2026-03-01 00:00:00');

INSERT INTO admin_user_roles (id, user_id, role_code, organization_id, assigned_at) VALUES
('role_004', 'usr_super_001', 'super_admin', NULL, '2026-03-01 00:00:00'),
('role_005', 'usr_client_viewer_001', 'client_viewer', 'org_busan_220', '2026-03-01 00:00:00'),
('role_006', 'usr_knowledge_editor_001', 'knowledge_editor', 'org_seoul_120', '2026-03-01 00:00:00');
