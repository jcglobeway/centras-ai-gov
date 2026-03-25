-- Daily Metrics Table
CREATE TABLE daily_metrics_org (
    id VARCHAR(255) PRIMARY KEY,
    metric_date DATE NOT NULL,
    organization_id VARCHAR(255) NOT NULL,
    service_id VARCHAR(255) NOT NULL,
    total_sessions INTEGER NOT NULL DEFAULT 0,
    total_questions INTEGER NOT NULL DEFAULT 0,
    resolved_rate DECIMAL(5,2),
    fallback_rate DECIMAL(5,2),
    zero_result_rate DECIMAL(5,2),
    avg_response_time_ms INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    FOREIGN KEY (service_id) REFERENCES services(id),
    UNIQUE (metric_date, organization_id, service_id)
);

CREATE INDEX idx_daily_metrics_org_date ON daily_metrics_org(metric_date);
CREATE INDEX idx_daily_metrics_org_organization_id ON daily_metrics_org(organization_id);

-- Seed development metrics
INSERT INTO daily_metrics_org (id, metric_date, organization_id, service_id, total_sessions, total_questions, resolved_rate, fallback_rate, zero_result_rate, avg_response_time_ms, created_at) VALUES
('metric_001', '2026-03-14', 'org_local_gov', 'svc_welfare', 120, 180, 85.50, 10.20, 4.30, 1200, '2026-03-15 00:00:00'),
('metric_002', '2026-03-14', 'org_central_gov', 'svc_faq', 80, 95, 78.30, 15.40, 6.30, 950, '2026-03-15 00:00:00');
