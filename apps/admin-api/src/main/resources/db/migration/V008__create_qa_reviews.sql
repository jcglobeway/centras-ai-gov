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
-- Note: V010에서 questions를 먼저 생성하므로 이제 FK가 작동함
-- 하지만 V008이 먼저 실행되므로 seed는 V012 이후에 추가하거나 여기서는 제거
-- INSERT는 V012 이후에 수동으로 추가하거나 테스트에서 생성
