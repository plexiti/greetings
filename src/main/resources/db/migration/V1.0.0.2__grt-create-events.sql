CREATE TABLE grt_events (
    type VARCHAR(128),
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    raised_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP,
    agg_id VARCHAR(36) NOT NULL,
    agg_type VARCHAR(128) NOT NULL,
    agg_version INTEGER NOT NULL,
    command_id VARCHAR(36) NOT NULL
);
