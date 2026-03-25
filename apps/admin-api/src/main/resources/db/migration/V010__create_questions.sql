-- Questions Table
CREATE TABLE questions (
    id VARCHAR(255) PRIMARY KEY,
    organization_id VARCHAR(255) NOT NULL,
    service_id VARCHAR(255) NOT NULL,
    chat_session_id VARCHAR(255) NOT NULL,
    question_text TEXT NOT NULL,
    question_intent_label VARCHAR(100),
    channel VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    FOREIGN KEY (service_id) REFERENCES services(id),
    FOREIGN KEY (chat_session_id) REFERENCES chat_sessions(id)
);

CREATE INDEX idx_questions_organization_id ON questions(organization_id);
CREATE INDEX idx_questions_service_id ON questions(service_id);
CREATE INDEX idx_questions_chat_session_id ON questions(chat_session_id);
CREATE INDEX idx_questions_created_at ON questions(created_at);

-- Seed development questions
INSERT INTO questions (id, organization_id, service_id, chat_session_id, question_text, question_intent_label, channel, created_at) VALUES
('question_001', 'org_local_gov', 'svc_welfare', 'chat_session_001', 'How do I apply for welfare benefits?', 'welfare_application', 'web', '2026-03-15 09:01:00'),
('question_002', 'org_central_gov', 'svc_faq', 'chat_session_002', 'What are the operating hours?', 'general_info', 'web', '2026-03-15 09:31:00'),
('question_003', 'org_local_gov', 'svc_welfare', 'chat_session_001', 'Where can I find welfare forms?', 'document_request', 'web', '2026-03-15 09:05:00');
