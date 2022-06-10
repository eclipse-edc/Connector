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


CREATE UNIQUE INDEX IF NOT EXISTS lease_lease_id_uindex
    ON edc_lease (lease_id);



CREATE TABLE IF NOT EXISTS edc_contract_agreement
(
    agreement_id      VARCHAR NOT NULL
        CONSTRAINT contract_agreement_pk
            PRIMARY KEY,
    provider_agent_id VARCHAR,
    consumer_agent_id VARCHAR,
    signing_date      BIGINT,
    start_date        BIGINT,
    end_date          INTEGER,
    asset_id          VARCHAR NOT NULL,
    policy            JSON
);


CREATE TABLE IF NOT EXISTS edc_contract_negotiation
(
    id                   VARCHAR                                            NOT NULL
        CONSTRAINT contract_negotiation_pk
            PRIMARY KEY,
    correlation_id       VARCHAR,
    counterparty_id      VARCHAR                                            NOT NULL,
    counterparty_address VARCHAR                                            NOT NULL,
    protocol             VARCHAR DEFAULT 'ids-multipart'::CHARACTER VARYING NOT NULL,
    type                 INTEGER DEFAULT 0                                  NOT NULL,
    state                INTEGER DEFAULT 0                                  NOT NULL,
    state_count          INTEGER DEFAULT 0,
    state_timestamp      BIGINT,
    error_detail         VARCHAR,
    agreement_id         VARCHAR
        CONSTRAINT contract_negotiation_contract_agreement_id_fk
            REFERENCES edc_contract_agreement,
    contract_offers      JSON,
    trace_context        JSON,
    lease_id             VARCHAR
        CONSTRAINT contract_negotiation_lease_lease_id_fk
            REFERENCES edc_lease
            ON DELETE SET NULL,
    CONSTRAINT provider_correlation_id CHECK (type = '0' OR correlation_id IS NOT NULL)
);

COMMENT ON COLUMN edc_contract_negotiation.agreement_id IS 'ContractAgreement serialized as JSON';

COMMENT ON COLUMN edc_contract_negotiation.contract_offers IS 'List<ContractOffer> serialized as JSON';

COMMENT ON COLUMN edc_contract_negotiation.trace_context IS 'Map<String,String> serialized as JSON';


CREATE INDEX IF NOT EXISTS contract_negotiation_correlationid_index
    ON edc_contract_negotiation (correlation_id);

CREATE UNIQUE INDEX IF NOT EXISTS contract_negotiation_id_uindex
    ON edc_contract_negotiation (id);

CREATE UNIQUE INDEX IF NOT EXISTS contract_agreement_id_uindex
    ON edc_contract_agreement (agreement_id);

-- creates a view that flattens the model and that can be used for easy querying.
-- initially, it is only used for dynamic QuerySpec support, but can be used for
CREATE VIEW edc_contract_negotiation_view
            (id, correlation_id, counterparty_id, counterparty_address, protocol, type, state, state_count,
             state_timestamp, error_detail, contract_offers, lease_id, trace_context, agreement_id, provider_agent_id,
             consumer_agent_id, signing_date, start_date, end_date, asset_id, policy)
AS
SELECT edc_contract_negotiation.id,
       edc_contract_negotiation.correlation_id,
       edc_contract_negotiation.counterparty_id,
       edc_contract_negotiation.counterparty_address,
       edc_contract_negotiation.protocol,
       edc_contract_negotiation.type,
       edc_contract_negotiation.state,
       edc_contract_negotiation.state_count,
       edc_contract_negotiation.state_timestamp,
       edc_contract_negotiation.error_detail,
       edc_contract_negotiation.contract_offers,
       edc_contract_negotiation.lease_id,
       edc_contract_negotiation.trace_context,
       agr.agreement_id,
       agr.provider_agent_id,
       agr.consumer_agent_id,
       agr.signing_date,
       agr.start_date,
       agr.end_date,
       agr.asset_id,
       agr.policy
FROM edc_contract_negotiation
         LEFT JOIN edc_contract_agreement agr
                   ON edc_contract_negotiation.agreement_id::TEXT = agr.agreement_id::TEXT;