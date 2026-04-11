CREATE TABLE redteam_batch_runs (
    id              VARCHAR(50)  NOT NULL PRIMARY KEY,
    organization_id VARCHAR(50)  NOT NULL,
    triggered_by    VARCHAR(50)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'pending',
    total_cases     INT          NOT NULL DEFAULT 0,
    pass_count      INT          NOT NULL DEFAULT 0,
    fail_count      INT          NOT NULL DEFAULT 0,
    pass_rate       DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    started_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ
);

CREATE TABLE redteam_case_results (
    id              VARCHAR(50)  NOT NULL PRIMARY KEY,
    batch_run_id    VARCHAR(50)  NOT NULL REFERENCES redteam_batch_runs(id),
    case_id         VARCHAR(50)  NOT NULL,
    query_text      TEXT         NOT NULL,
    response_text   TEXT         NOT NULL DEFAULT '',
    answer_status   VARCHAR(30)  NOT NULL DEFAULT '',
    judgment        VARCHAR(10)  NOT NULL,
    judgment_detail TEXT,
    executed_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_redteam_batch_runs_org ON redteam_batch_runs (organization_id, started_at DESC);
CREATE INDEX idx_redteam_case_results_run ON redteam_case_results (batch_run_id, judgment);
