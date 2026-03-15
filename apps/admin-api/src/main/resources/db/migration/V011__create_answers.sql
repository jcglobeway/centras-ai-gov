-- Answers Table
CREATE TABLE answers (
    id VARCHAR(255) PRIMARY KEY,
    question_id VARCHAR(255) NOT NULL,
    answer_text TEXT NOT NULL,
    answer_status VARCHAR(50) NOT NULL,
    response_time_ms INTEGER,
    citation_count INTEGER,
    fallback_reason_code VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (question_id) REFERENCES questions(id)
);

CREATE INDEX idx_answers_question_id ON answers(question_id);
CREATE INDEX idx_answers_answer_status ON answers(answer_status);
CREATE INDEX idx_answers_created_at ON answers(created_at);

-- Seed development answers
INSERT INTO answers (id, question_id, answer_text, answer_status, response_time_ms, citation_count, fallback_reason_code, created_at) VALUES
('answer_001', 'question_001', 'You can apply for welfare benefits at...', 'answered', 1200, 3, NULL, '2026-03-15 09:01:05'),
('answer_002', 'question_002', 'I could not find relevant information.', 'no_answer', 800, 0, 'ZERO_RESULT', '2026-03-15 09:31:05'),
('answer_003', 'question_003', 'General fallback answer...', 'fallback', 500, 0, 'LOW_CONFIDENCE', '2026-03-15 09:05:05');
