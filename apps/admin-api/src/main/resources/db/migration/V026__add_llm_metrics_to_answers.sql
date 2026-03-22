ALTER TABLE answers ADD COLUMN model_name VARCHAR(100);
ALTER TABLE answers ADD COLUMN provider_name VARCHAR(50);
ALTER TABLE answers ADD COLUMN input_tokens INTEGER;
ALTER TABLE answers ADD COLUMN output_tokens INTEGER;
ALTER TABLE answers ADD COLUMN total_tokens INTEGER;
ALTER TABLE answers ADD COLUMN estimated_cost_usd DOUBLE PRECISION;
ALTER TABLE answers ADD COLUMN finish_reason VARCHAR(50);
