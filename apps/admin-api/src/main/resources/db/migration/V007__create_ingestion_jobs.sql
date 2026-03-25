-- Ingestion Jobs Table
CREATE TABLE ingestion_jobs (
    id VARCHAR(255) PRIMARY KEY,
    organization_id VARCHAR(255) NOT NULL,
    service_id VARCHAR(255) NOT NULL,
    crawl_source_id VARCHAR(255) NOT NULL,
    document_id VARCHAR(255),
    document_version_id VARCHAR(255),
    job_type VARCHAR(50) NOT NULL,
    job_status VARCHAR(50) NOT NULL,
    job_stage VARCHAR(50) NOT NULL,
    trigger_type VARCHAR(50) NOT NULL,
    runner_type VARCHAR(50) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 1,
    error_code VARCHAR(100),
    requested_at TIMESTAMP NOT NULL,
    requested_by VARCHAR(255),
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    FOREIGN KEY (service_id) REFERENCES services(id),
    FOREIGN KEY (crawl_source_id) REFERENCES crawl_sources(id)
);

CREATE INDEX idx_ingestion_jobs_organization_id ON ingestion_jobs(organization_id);
CREATE INDEX idx_ingestion_jobs_service_id ON ingestion_jobs(service_id);
CREATE INDEX idx_ingestion_jobs_crawl_source_id ON ingestion_jobs(crawl_source_id);
CREATE INDEX idx_ingestion_jobs_job_status ON ingestion_jobs(job_status);
CREATE INDEX idx_ingestion_jobs_requested_at ON ingestion_jobs(requested_at);

-- Seed development ingestion jobs
INSERT INTO ingestion_jobs (id, organization_id, service_id, crawl_source_id, document_id, job_type, job_status, job_stage, trigger_type, runner_type, attempt_count, error_code, requested_at, started_at, finished_at, created_at) VALUES
('ing_job_101', 'org_acc',         'svc_acc_chatbot', 'crawl_src_001', 'doc_301', 'crawl', 'succeeded', 'complete', 'scheduled', 'python_worker', 1, NULL,            '2026-03-15 01:00:00', '2026-03-15 01:01:00', '2026-03-15 01:20:00', '2026-03-15 01:00:00'),
('ing_job_202', 'org_central_gov', 'svc_faq',         'crawl_src_002', NULL,      'crawl', 'failed',    'complete', 'manual',    'python_worker', 2, 'CRAWL_TIMEOUT', '2026-03-14 22:00:00', '2026-03-14 22:01:00', '2026-03-14 22:03:00', '2026-03-14 22:00:00');
