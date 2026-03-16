-- Feedbacks Table: 시민 만족도 피드백
CREATE TABLE feedbacks (
    id VARCHAR(255) PRIMARY KEY,
    organization_id VARCHAR(255) NOT NULL,
    service_id VARCHAR(255) NOT NULL,
    question_id VARCHAR(255),
    session_id VARCHAR(255),
    rating INTEGER NOT NULL,
    comment TEXT,
    channel VARCHAR(50),
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    FOREIGN KEY (service_id) REFERENCES services(id),
    FOREIGN KEY (question_id) REFERENCES questions(id),
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
);

CREATE INDEX idx_feedbacks_organization_id ON feedbacks(organization_id);
CREATE INDEX idx_feedbacks_service_id ON feedbacks(service_id);
CREATE INDEX idx_feedbacks_question_id ON feedbacks(question_id);
CREATE INDEX idx_feedbacks_submitted_at ON feedbacks(submitted_at);
