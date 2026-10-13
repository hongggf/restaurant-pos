-- Payments attach to the whole table session, not a single round (cashier spec §6).
-- v1: one payment covers the bill; kept as a list so split payment needs no schema change.
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES table_sessions (id),
    method VARCHAR(20) NOT NULL,
    bill_total NUMERIC(12, 2) NOT NULL,
    amount_tendered NUMERIC(14, 2) NOT NULL,
    tendered_currency VARCHAR(3) NOT NULL,
    change_usd NUMERIC(12, 2),
    change_khr NUMERIC(14, 2),
    reference_note VARCHAR(200),
    paid_by UUID REFERENCES users (id),
    paid_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_payments_session_id ON payments (session_id);
CREATE INDEX IF NOT EXISTS idx_payments_paid_at ON payments (paid_at);
