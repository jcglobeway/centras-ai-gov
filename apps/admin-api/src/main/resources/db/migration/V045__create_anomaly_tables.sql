-- alert_thresholds: 지표별 임계값 설정
CREATE TABLE alert_thresholds (
    metric_key        VARCHAR(64)    PRIMARY KEY,
    warn_value        NUMERIC(10, 4) NOT NULL,
    critical_value    NUMERIC(10, 4) NOT NULL,
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

-- alert_events: 임계값 초과 이벤트 기록
CREATE TABLE alert_events (
    id             VARCHAR(32)    PRIMARY KEY,
    metric_key     VARCHAR(64)    NOT NULL,
    current_value  NUMERIC(10, 4) NOT NULL,
    severity       VARCHAR(16)    NOT NULL,  -- 'warn' | 'critical'
    triggered_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

-- 기본 임계값 시드
INSERT INTO alert_thresholds (metric_key, warn_value, critical_value) VALUES
    ('fallback_rate',        10.0, 15.0),
    ('zero_result_rate',      5.0,  8.0),
    ('avg_response_time_ms', 1500, 2500);
