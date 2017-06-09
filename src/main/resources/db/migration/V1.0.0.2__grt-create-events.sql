CREATE TABLE grt_events (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    type VARCHAR(128) NOT NULL,
    raised_at TIMESTAMP NOT NULL,
    properties TEXT NOT NULL,
    command_id VARCHAR(36) NOT NULL,
    agg_id VARCHAR(36) NOT NULL,
    agg_type VARCHAR(128) NOT NULL,
    agg_version INTEGER NOT NULL,
    published_at TIMESTAMP
);
