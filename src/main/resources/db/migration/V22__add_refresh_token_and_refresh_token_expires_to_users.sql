ALTER TABLE users
    ADD COLUMN refresh_token VARCHAR(255) UNIQUE,
    ADD COLUMN refresh_token_expires TIMESTAMP;
