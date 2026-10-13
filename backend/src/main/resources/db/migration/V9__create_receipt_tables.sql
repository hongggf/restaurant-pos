-- Phase 8: receipts. One receipt per payment, numbered from a global sequence.
-- PDFs are rendered on demand — nothing is stored on disk.

CREATE SEQUENCE receipt_number_seq START WITH 1;

CREATE TABLE receipts (
    id              UUID PRIMARY KEY,
    payment_id      UUID        NOT NULL UNIQUE REFERENCES payments (id),
    receipt_number  VARCHAR(20) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);
