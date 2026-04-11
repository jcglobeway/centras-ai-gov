CREATE TABLE redteam_cases (
    id              VARCHAR(50)  NOT NULL PRIMARY KEY,
    organization_id VARCHAR(50)  NOT NULL,
    category        VARCHAR(50)  NOT NULL,
    title           VARCHAR(100) NOT NULL,
    query_text      TEXT         NOT NULL,
    expected_behavior VARCHAR(20) NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by      VARCHAR(50)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_redteam_cases_org ON redteam_cases (organization_id, created_at DESC);
