-- Post-Login Runtime Risk Detection Engine - PostgreSQL schema
-- Run this once against your database before starting the application (ddl-auto=validate expects tables to exist).

CREATE TABLE IF NOT EXISTS raw_signals (
    id SERIAL PRIMARY KEY,
    session_id VARCHAR(100),
    user_id VARCHAR(100),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    signal_json JSONB
);

CREATE TABLE IF NOT EXISTS risk_decisions (
    id SERIAL PRIMARY KEY,
    session_id VARCHAR(100),
    user_id VARCHAR(100),
    risk_score INTEGER,
    decision VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
