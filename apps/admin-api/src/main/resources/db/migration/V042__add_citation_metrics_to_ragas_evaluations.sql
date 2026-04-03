ALTER TABLE ragas_evaluations
    ADD COLUMN citation_coverage    DOUBLE PRECISION,
    ADD COLUMN citation_correctness DOUBLE PRECISION;
