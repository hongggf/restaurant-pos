CREATE TABLE IF NOT EXISTS currencies (
    id UUID PRIMARY KEY,
    code VARCHAR(3) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS exchange_rates (
    id UUID PRIMARY KEY,
    from_currency_id UUID NOT NULL REFERENCES currencies (id),
    to_currency_id UUID NOT NULL REFERENCES currencies (id),
    rate NUMERIC(18, 6) NOT NULL,
    effective_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

INSERT INTO currencies (id, code, name, symbol, is_default, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'USD', 'US Dollar', '$', TRUE, now(), now()),
    (gen_random_uuid(), 'KHR', 'Cambodian Riel', '៛', FALSE, now(), now());

INSERT INTO exchange_rates (id, from_currency_id, to_currency_id, rate, effective_date, created_at, updated_at)
SELECT gen_random_uuid(), usd.id, khr.id, 4100, CURRENT_DATE, now(), now()
FROM currencies usd, currencies khr
WHERE usd.code = 'USD' AND khr.code = 'KHR';

INSERT INTO exchange_rates (id, from_currency_id, to_currency_id, rate, effective_date, created_at, updated_at)
SELECT gen_random_uuid(), khr.id, usd.id, 1 / 4100.0, CURRENT_DATE, now(), now()
FROM currencies usd, currencies khr
WHERE usd.code = 'USD' AND khr.code = 'KHR';
