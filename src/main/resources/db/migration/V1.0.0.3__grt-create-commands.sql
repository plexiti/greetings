CREATE TABLE grt_commands (
    type VARCHAR(128),
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    issued_at TIMESTAMP NOT NULL,
    issued_by VARCHAR(256)
);
