CREATE TABLE grt_greetings (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    version INTEGER NOT NULL,
    greeting VARCHAR(256) UNIQUE NOT NULL,
    contacts INTEGER NOT NULL
);
