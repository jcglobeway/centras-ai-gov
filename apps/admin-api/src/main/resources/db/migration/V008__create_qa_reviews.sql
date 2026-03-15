-- QA Reviews Table
CREATE TABLE qa_reviews (
    id VARCHAR(255) PRIMARY KEY,
    question_id VARCHAR(255) NOT NULL,
    review_status VARCHAR(50) NOT NULL,
    root_cause_code VARCHAR(100),
    action_type VARCHAR(100),
    action_target_id VARCHAR(255),
    review_comment TEXT,
    reviewer_id VARCHAR(255) NOT NULL,
    reviewed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    -- FK는 questions 테이블 구현 후 추가
    -- FOREIGN KEY (question_id) REFERENCES questions(id)
    -- FOREIGN KEY (reviewer_id) REFERENCES admin_users(id)
);

CREATE INDEX idx_qa_reviews_question_id ON qa_reviews(question_id);
CREATE INDEX idx_qa_reviews_review_status ON qa_reviews(review_status);
CREATE INDEX idx_qa_reviews_reviewer_id ON qa_reviews(reviewer_id);
CREATE INDEX idx_qa_reviews_reviewed_at ON qa_reviews(reviewed_at);

-- Seed development QA reviews
-- Note: question_id는 아직 questions 테이블이 없으므로 임시 ID 사용
INSERT INTO qa_reviews (id, question_id, review_status, root_cause_code, action_type, review_comment, reviewer_id, reviewed_at, created_at) VALUES
('qa_rev_001', 'question_001', 'confirmed_issue', 'missing_document', 'document_fix_request', 'Seoul welfare document is missing', 'usr_qa_001', '2026-03-15 10:00:00', '2026-03-15 10:00:00'),
('qa_rev_002', 'question_002', 'resolved', NULL, NULL, 'Fixed after document update', 'usr_qa_001', '2026-03-15 11:00:00', '2026-03-15 11:00:00');
