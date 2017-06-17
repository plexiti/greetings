CREATE TABLE grt_commands (
    type VARCHAR(16) NOT NULL,
    name VARCHAR(128),
    origin VARCHAR(64) NOT NULL,
    target VARCHAR(64) NOT NULL,
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    definition INTEGER NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    triggered_by VARCHAR(36),
    flow_id VARCHAR(36),
    correlation_id VARCHAR(128),
    async BOOLEAN NOT NULL,
    completed_by VARCHAR(36),
    json TEXT NOT NULL,
    published_at TIMESTAMP
);
