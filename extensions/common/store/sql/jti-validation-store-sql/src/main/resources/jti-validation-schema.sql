CREATE TABLE IF NOT EXISTS edc_jti_validation
(
    token_id   VARCHAR NOT NULL PRIMARY KEY,
    expires_at BIGINT -- expiry time in epoch millis
);

-- Supports the periodic deletion of expired entries
CREATE INDEX IF NOT EXISTS jti_validation_expires_at_index
    ON edc_jti_validation (expires_at);
