-- Add FK constraints to qa_reviews table
ALTER TABLE qa_reviews
ADD CONSTRAINT fk_qa_reviews_question_id
FOREIGN KEY (question_id) REFERENCES questions(id);

-- reviewer_id FK는 디버그 세션 user가 DB에 없을 수 있으므로 임시로 제거
-- ALTER TABLE qa_reviews
-- ADD CONSTRAINT fk_qa_reviews_reviewer_id
-- FOREIGN KEY (reviewer_id) REFERENCES admin_users(id);

-- Seed development QA reviews (이제 FK가 있으므로 questions가 먼저 생성됨)
INSERT INTO qa_reviews (id, question_id, review_status, root_cause_code, action_type, review_comment, reviewer_id, reviewed_at, created_at) VALUES
('qa_rev_001', 'question_002', 'confirmed_issue', 'missing_document', 'document_fix_request', 'No answer found - missing document', 'usr_qa_001', '2026-03-15 10:00:00', '2026-03-15 10:00:00'),
('qa_rev_002', 'question_003', 'pending', NULL, NULL, 'Need more review for fallback case', 'usr_qa_001', '2026-03-15 11:00:00', '2026-03-15 11:00:00');
