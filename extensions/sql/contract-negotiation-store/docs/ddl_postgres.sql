-- Statements are designed for and tested with Postgres only!

CREATE TABLE IF NOT EXISTS edc_lease
(
    leased_by      VARCHAR               NOT NULL,
    leased_at      BIGINT,
    lease_duration INTEGER DEFAULT 60000 NOT NULL,
    lease_id       VARCHAR               NOT NULL
        CONSTRAINT lease_pk
            PRIMARY KEY
);

COMMENT ON COLUMN edc_lease.leased_at IS 'posix timestamp of lease';

COMMENT ON COLUMN edc_lease.lease_duration IS 'duration of lease in milliseconds';


CREATE UNIQUE INDEX lease_lease_id_uindex
    ON edc_lease (lease_id);



CREATE TABLE IF NOT EXISTS edc_contract_agreement
(
    id                VARCHAR NOT NULL
        CONSTRAINT contract_agreement_pk
            PRIMARY KEY,
    provider_agent_id VARCHAR,
    consumer_agent_id VARCHAR,
    signing_date      BIGINT,
    start_date        BIGINT,
    end_date          INTEGER,
    asset_id          VARCHAR NOT NULL,
    policy_id         VARCHAR
);


CREATE TABLE IF NOT EXISTS edc_contract_negotiation
(
    id                    VARCHAR                                            NOT NULL
        CONSTRAINT contract_negotiation_pk
            PRIMARY KEY,
    correlation_id        VARCHAR                                            NOT NULL,
    counterparty_id       VARCHAR                                            NOT NULL,
    counterparty_address  VARCHAR                                            NOT NULL,
    protocol              VARCHAR DEFAULT 'ids-multipart'::CHARACTER VARYING NOT NULL,
    type                  INTEGER DEFAULT 0                                  NOT NULL,
    state                 INTEGER DEFAULT 0                                  NOT NULL,
    state_count           INTEGER DEFAULT 0,
    state_timestamp       BIGINT,
    error_detail          VARCHAR,
    contract_agreement_id VARCHAR
        CONSTRAINT contract_negotiation_contract_agreement_id_fk
            REFERENCES edc_contract_agreement,
    contract_offers       VARCHAR,
    trace_context         VARCHAR,
    lease_id              VARCHAR
        CONSTRAINT contract_negotiation_lease_lease_id_fk
            REFERENCES edc_lease
            ON DELETE SET NULL
);

COMMENT ON COLUMN edc_contract_negotiation.contract_agreement_id IS 'ContractAgreement serialized as JSON';

COMMENT ON COLUMN edc_contract_negotiation.contract_offers IS 'List<ContractOffer> serialized as JSON';

COMMENT ON COLUMN edc_contract_negotiation.trace_context IS 'Map<String,String> serialized as JSON';


CREATE INDEX IF NOT EXISTS contract_negotiation_correlationid_index
    ON edc_contract_negotiation (correlation_id);

CREATE UNIQUE INDEX IF NOT EXISTS contract_negotiation_id_uindex
    ON edc_contract_negotiation (id);

CREATE UNIQUE INDEX IF NOT EXISTS contract_agreement_id_uindex
    ON edc_contract_agreement (id);

