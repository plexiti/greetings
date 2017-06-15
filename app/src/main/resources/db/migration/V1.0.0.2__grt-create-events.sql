CREATE TABLE grt_events (
    type VARCHAR(16) NOT NULL,
    name VARCHAR(128) NOT NULL,
    origin VARCHAR(64) NOT NULL,
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    definition INTEGER NOT NULL,
    raised_at TIMESTAMP NOT NULL,
    command_id VARCHAR(36),
    agg_id VARCHAR(36) NOT NULL,
    agg_type VARCHAR(128) NOT NULL,
    agg_version INTEGER NOT NULL,
    json TEXT NOT NULL,
    published_at TIMESTAMP
);
