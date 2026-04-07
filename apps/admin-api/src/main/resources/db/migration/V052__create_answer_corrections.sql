CREATE TABLE answer_corrections (
    id                    VARCHAR(64)  NOT NULL PRIMARY KEY,
    organization_id       VARCHAR(64)  NOT NULL,
    service_id            VARCHAR(64)  NOT NULL,
    question_id           VARCHAR(64)  NOT NULL,
    question_text         TEXT         NOT NULL,
    original_answer_text  TEXT,
    corrected_answer_text TEXT         NOT NULL,
    corrected_by          VARCHAR(255) NOT NULL,
    correction_reason     TEXT,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_answer_corrections_organization_id ON answer_corrections (organization_id);
CREATE INDEX idx_answer_corrections_question_id     ON answer_corrections (question_id);
