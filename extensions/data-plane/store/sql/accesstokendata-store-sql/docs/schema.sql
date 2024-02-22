-- Statements are designed for and tested with Postgres only!

CREATE TABLE IF NOT EXISTS edc_accesstokendata
(
    id           VARCHAR NOT NULL PRIMARY KEY,
    claim_token  json    NOT NULL,
    data_address JSON    NOT NULL
);

COMMENT ON COLUMN edc_accesstokendata.claim_token IS 'ClaimToken serialized as JSON map';
COMMENT ON COLUMN edc_accesstokendata.data_address IS 'DataAddress serialized as JSON map';
