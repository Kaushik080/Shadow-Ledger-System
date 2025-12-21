-- Ledger table for Shadow Ledger Service (immutable, append-only)
CREATE TABLE IF NOT EXISTS ledger (
    event_id VARCHAR(255) PRIMARY KEY,
    account_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('debit', 'credit')),
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for efficient queries and window functions
CREATE INDEX IF NOT EXISTS idx_ledger_account_id ON ledger(account_id);
CREATE INDEX IF NOT EXISTS idx_ledger_timestamp ON ledger(timestamp);
CREATE INDEX IF NOT EXISTS idx_ledger_account_timestamp_eventid ON ledger(account_id, timestamp, event_id);

