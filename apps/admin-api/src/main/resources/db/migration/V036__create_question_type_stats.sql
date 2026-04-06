CREATE TABLE question_type_stats (
  id TEXT PRIMARY KEY,
  run_date DATE NOT NULL,
  organization_id TEXT NOT NULL,
  type_label TEXT NOT NULL,
  count INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_qts_org_date ON question_type_stats (organization_id, run_date);
