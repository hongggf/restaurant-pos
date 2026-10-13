CREATE TABLE IF NOT EXISTS dining_tables (
    id UUID PRIMARY KEY,
    table_number VARCHAR(20) NOT NULL,
    qr_token VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_dining_tables_table_number UNIQUE (table_number),
    CONSTRAINT uq_dining_tables_qr_token UNIQUE (qr_token)
);

CREATE TABLE IF NOT EXISTS table_sessions (
    id UUID PRIMARY KEY,
    table_id UUID NOT NULL REFERENCES dining_tables (id),
    status VARCHAR(20) NOT NULL,
    last_activity_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- One ACTIVE session per table; CLOSED sessions accumulate as history
CREATE UNIQUE INDEX IF NOT EXISTS uq_table_sessions_one_active
    ON table_sessions (table_id) WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_table_sessions_table_id ON table_sessions (table_id);
CREATE INDEX IF NOT EXISTS idx_table_sessions_status ON table_sessions (status);
