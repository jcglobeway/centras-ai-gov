-- V035: 임베딩 기반 유사 질문 클러스터 결과 저장 테이블 (PostgreSQL only)
CREATE TABLE question_similarity_groups (
  id                  TEXT      PRIMARY KEY,
  run_date            DATE      NOT NULL,
  organization_id     TEXT      NOT NULL,
  representative_text TEXT      NOT NULL,
  question_count      INT       NOT NULL,
  avg_similarity      FLOAT     NOT NULL,
  sample_texts        TEXT      NOT NULL,  -- JSON array, 최대 5개
  created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_qsg_org_date ON question_similarity_groups (organization_id, run_date);
