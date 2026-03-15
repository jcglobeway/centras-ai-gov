-- Audit Logs Table
CREATE TABLE audit_logs (
    id VARCHAR(255) PRIMARY KEY,
    actor_user_id VARCHAR(255),
    actor_role_code VARCHAR(50),
    organization_id VARCHAR(255),
    action_code VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(255),
    request_id VARCHAR(255),
    trace_id VARCHAR(255),
    result_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (actor_user_id) REFERENCES admin_users(id)
);

CREATE INDEX idx_audit_logs_actor_user_id ON audit_logs(actor_user_id);
CREATE INDEX idx_audit_logs_action_code ON audit_logs(action_code);
CREATE INDEX idx_audit_logs_organization_id ON audit_logs(organization_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_request_id ON audit_logs(request_id);
CREATE INDEX idx_audit_logs_trace_id ON audit_logs(trace_id);
