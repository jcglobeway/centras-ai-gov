CREATE TABLE ragas_evaluations (
    id VARCHAR(50) PRIMARY KEY,
    question_id VARCHAR(50) NOT NULL,
    faithfulness DOUBLE PRECISION,
    answer_relevancy DOUBLE PRECISION,
    context_precision DOUBLE PRECISION,
    context_recall DOUBLE PRECISION,
    evaluated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    judge_provider VARCHAR(50),
    judge_model VARCHAR(100)
);

CREATE INDEX idx_ragas_evaluations_question_id ON ragas_evaluations(question_id);
