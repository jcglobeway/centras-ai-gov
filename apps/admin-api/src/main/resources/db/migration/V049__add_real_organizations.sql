-- ── 운영 기관 3개 추가 ────────────────────────────────────────────────────────
-- 운영 DB(jcg-prod4 MySQL)의 s_services 테이블 기준으로 실제 서비스 중인 기관을 등록한다.
-- 기존 테스트 기관(org_acc, org_central_gov, org_local_gov)은 데모 데이터가 연결되어 있어
-- 삭제하지 않고 crawl_source만 비활성화한다.

-- ── organizations ──────────────────────────────────────────────────────────────

INSERT INTO organizations (id, name, org_code, status, institution_type, created_at) VALUES
('org_ggc',   '경기도의회',        'GGC_COUNCIL',    'active', 'local_government',   NOW()),
('org_gjf',   '경기도일자리재단',  'GJF_FOUNDATION', 'active', 'public_institution', NOW()),
('org_namgu', '광주광역시 남구청', 'GWANGJU_NAMGU',  'active', 'local_government',   NOW());

-- ── services ───────────────────────────────────────────────────────────────────

INSERT INTO services (id, organization_id, name, channel_type, status, go_live_at, created_at) VALUES
('svc_ggc_chatbot',   'org_ggc',   '경기도의회 챗봇',        'web', 'active', NOW(), NOW()),
('svc_gjf_chatbot',   'org_gjf',   '경기도일자리재단 챗봇',  'web', 'active', NOW(), NOW()),
('svc_namgu_chatbot', 'org_namgu', '광주남구청 민원 챗봇',   'web', 'active', NOW(), NOW());

-- ── crawl_sources ──────────────────────────────────────────────────────────────
-- 기존 테스트 크롤소스는 paused 처리

UPDATE crawl_sources
   SET status = 'paused', is_active = false
 WHERE id IN ('crawl_src_001', 'crawl_src_002', 'crawl_src_003');

-- 실제 운영 사이트 크롤소스 추가
-- 경기도의회: ggc.go.kr → GgcGoKrAdapter 자동 감지
INSERT INTO crawl_sources (id, organization_id, service_id, name, source_type, source_uri, collection_mode, render_mode, schedule_expr, is_active, status, created_at) VALUES
('crawl_src_ggc',          'org_ggc',   'svc_ggc_chatbot',   '경기도의회 공식사이트',          'website', 'https://www.ggc.go.kr',                                    'full', 'browser_playwright', '0 2 * * *', true, 'active', NOW()),
('crawl_src_gjf',          'org_gjf',   'svc_gjf_chatbot',   '경기도일자리재단 공식사이트',    'website', 'https://www.gjf.or.kr',                                    'full', 'browser_playwright', '0 2 * * *', true, 'active', NOW()),
('crawl_src_gjf_jobaba',   'org_gjf',   'svc_gjf_chatbot',   '경기도일자리재단 Jobaba 포털',   'website', 'https://job.gg.go.kr',                                     'full', 'browser_playwright', '0 2 * * *', true, 'active', NOW()),
('crawl_src_namgu',        'org_namgu', 'svc_namgu_chatbot', '광주남구청 공식사이트',          'website', 'https://www.namgu.gwangju.kr',                             'full', 'browser_playwright', '0 2 * * *', true, 'active', NOW()),
('crawl_src_namgu_sitemap','org_namgu', 'svc_namgu_chatbot', '광주남구청 사이트맵',            'sitemap', 'https://www.namgu.gwangju.kr/sitemap.es?mid=a10805000000', 'full', 'http_static',        '0 2 * * *', true, 'active', NOW());

-- ── admin_users ────────────────────────────────────────────────────────────────
-- 개발 환경은 DevelopmentAdminCredentialAuthenticator를 사용 (비밀번호: pass1234 통일)

INSERT INTO admin_users (id, email, display_name, status, created_at) VALUES
('usr_ggc_admin',   'admin@ggc.go.kr',          '경기도의회 관리자',       'active', NOW()),
('usr_gjf_admin',   'admin@gjf.or.kr',           '경기도일자리재단 관리자', 'active', NOW()),
('usr_namgu_admin', 'admin@namgu.gwangju.kr',    '광주남구청 관리자',       'active', NOW());

-- ── admin_user_roles ───────────────────────────────────────────────────────────

INSERT INTO admin_user_roles (id, user_id, role_code, organization_id, assigned_at) VALUES
('role_ggc_01',   'usr_ggc_admin',   'client_org_admin', 'org_ggc',   NOW()),
('role_gjf_01',   'usr_gjf_admin',   'client_org_admin', 'org_gjf',   NOW()),
('role_namgu_01', 'usr_namgu_admin', 'client_org_admin', 'org_namgu', NOW());
