-- V034: LLM 기반 키워드 추출 결과 저장 테이블 (PostgreSQL only)
CREATE TABLE question_keyword_stats (
  id              TEXT      PRIMARY KEY,
  run_date        DATE      NOT NULL,
  organization_id TEXT      NOT NULL,
  keyword         TEXT      NOT NULL,
  count           INT       NOT NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_qks_org_date ON question_keyword_stats (organization_id, run_date);
