CREATE TABLE greeting (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(256) NOT NULL
);

CREATE SEQUENCE greeting_sequence START WITH 1 INCREMENT BY 1;