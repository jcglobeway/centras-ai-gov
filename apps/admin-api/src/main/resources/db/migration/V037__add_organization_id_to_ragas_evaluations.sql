ALTER TABLE ragas_evaluations ADD COLUMN organization_id TEXT;
CREATE INDEX idx_ragas_eval_org ON ragas_evaluations (organization_id);
CREATE INDEX idx_ragas_eval_org_date ON ragas_evaluations (organization_id, evaluated_at);