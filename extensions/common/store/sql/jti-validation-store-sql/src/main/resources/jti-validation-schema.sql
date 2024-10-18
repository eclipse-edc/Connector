CREATE TABLE IF NOT EXISTS edc_jti_validation
(
    token_id   VARCHAR NOT NULL PRIMARY KEY,
    expires_at BIGINT -- expiry time in epoch millis
);


