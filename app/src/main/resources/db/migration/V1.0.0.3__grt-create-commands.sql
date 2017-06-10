CREATE TABLE grt_commands (
    message VARCHAR(16) NOT NULL,
    origin VARCHAR(64) NOT NULL,
    target VARCHAR(64) NOT NULL,
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    type VARCHAR(128),
    definition INTEGER NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    issued_by VARCHAR(256),
    json TEXT NOT NULL,
    published_at TIMESTAMP
);
