CREATE TABLE IF NOT EXISTS approval_requests (
    id BIGSERIAL PRIMARY KEY,
    agent_id VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMPTZ NULL
);

CREATE INDEX IF NOT EXISTS idx_approval_requests_status_requested_at
    ON approval_requests (status, requested_at DESC);