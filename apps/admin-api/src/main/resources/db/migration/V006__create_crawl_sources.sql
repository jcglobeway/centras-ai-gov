-- Crawl Sources Table
CREATE TABLE crawl_sources (
    id VARCHAR(255) PRIMARY KEY,
    organization_id VARCHAR(255) NOT NULL,
    service_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_uri TEXT NOT NULL,
    collection_mode VARCHAR(50) NOT NULL,
    render_mode VARCHAR(50) NOT NULL,
    schedule_expr VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    status VARCHAR(50) NOT NULL,
    last_crawled_at TIMESTAMP,
    last_succeeded_at TIMESTAMP,
    last_job_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    FOREIGN KEY (service_id) REFERENCES services(id)
);

CREATE INDEX idx_crawl_sources_organization_id ON crawl_sources(organization_id);
CREATE INDEX idx_crawl_sources_service_id ON crawl_sources(service_id);
CREATE INDEX idx_crawl_sources_status ON crawl_sources(status);
CREATE INDEX idx_crawl_sources_is_active ON crawl_sources(is_active);

-- Seed development crawl sources
INSERT INTO crawl_sources (id, organization_id, service_id, name, source_type, source_uri, collection_mode, render_mode, schedule_expr, is_active, status, last_succeeded_at, last_job_id, created_at) VALUES
('crawl_src_001', 'org_local_gov',   'svc_welfare',     'Seoul Notices',               'website', 'https://seoul.example.go.kr/notices', 'incremental', 'browser_playwright', '0 */6 * * *',  true,  'active', '2026-03-15 01:20:00', 'ing_job_101', '2026-03-01 00:00:00'),
('crawl_src_002', 'org_central_gov', 'svc_faq',         '중앙행정기관 민원 사이트맵',  'sitemap', 'https://www.gov.kr/sitemap.xml',  'incremental', 'http_static',        '0 */12 * * *', false, 'paused', '2026-03-14 22:10:00', 'ing_job_202', '2026-03-01 00:00:00'),
('crawl_src_003', 'org_local_gov',   'svc_welfare',     '지방행정기관 민원 포털',      'website', 'https://www.mois.go.kr/notices', 'incremental', 'browser_playwright', '0 */6 * * *',  true,  'active', '2026-03-15 01:20:00', NULL,          '2026-03-01 00:00:00');
