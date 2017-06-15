CREATE TABLE grt_commands (
    type VARCHAR(16) NOT NULL,
    name VARCHAR(128),
    origin VARCHAR(64) NOT NULL,
    target VARCHAR(64) NOT NULL,
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    definition INTEGER NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    issued_by VARCHAR(256),
    json TEXT NOT NULL,
    published_at TIMESTAMP
);
