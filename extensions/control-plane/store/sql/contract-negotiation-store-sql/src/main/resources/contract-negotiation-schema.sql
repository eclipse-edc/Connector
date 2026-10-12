-- Statements are designed for and tested with Postgres only!

CREATE TABLE IF NOT EXISTS edc_lease
(
    leased_by      VARCHAR               NOT NULL,
    leased_at      BIGINT,
    lease_duration INTEGER DEFAULT 60000 NOT NULL,
    resource_id       VARCHAR NOT NULL,
    resource_kind  VARCHAR NOT NULL,
    PRIMARY KEY(resource_id, resource_kind)
);

COMMENT ON COLUMN edc_lease.leased_at IS 'posix timestamp of lease';
COMMENT ON COLUMN edc_lease.lease_duration IS 'duration of lease in milliseconds';

CREATE TABLE IF NOT EXISTS edc_contract_agreement
(
    agr_id            VARCHAR NOT NULL
        CONSTRAINT contract_agreement_pk
            PRIMARY KEY,
    provider_agent_id VARCHAR,
    consumer_agent_id VARCHAR,
    signing_date      BIGINT,
    start_date        BIGINT,
    end_date          INTEGER,
    asset_id          VARCHAR NOT NULL,
    policy            JSON,
    agr_participant_context_id VARCHAR NOT NULL,
    agr_agreement_id VARCHAR NOT NULL,
    claims            JSON,
    retired           BOOLEAN DEFAULT FALSE NOT NULL,
    retirement_reason VARCHAR,
    retirement_date   BIGINT  DEFAULT 0 NOT NULL,
    UNIQUE (agr_agreement_id, agr_participant_context_id)
);

CREATE TABLE IF NOT EXISTS edc_contract_negotiation
(
    id                   VARCHAR           NOT NULL
        CONSTRAINT contract_negotiation_pk
            PRIMARY KEY,
    created_at           BIGINT            NOT NULL,
    updated_at           BIGINT            NOT NULL,
    correlation_id       VARCHAR,
    counterparty_id      VARCHAR           NOT NULL,
    counterparty_address VARCHAR           NOT NULL,
    protocol             VARCHAR           NOT NULL,
    type                 VARCHAR           NOT NULL,
    state                INTEGER DEFAULT 0 NOT NULL,
    state_count          INTEGER DEFAULT 0,
    state_timestamp      BIGINT,
    error_detail         VARCHAR,
    agreement_id         VARCHAR
        CONSTRAINT contract_negotiation_contract_agreement_id_fk
            REFERENCES edc_contract_agreement,
    contract_offers      JSON,
    callback_addresses   JSON,
    trace_context        JSON,
    pending              BOOLEAN DEFAULT FALSE,
    protocol_messages    JSON,
    participant_context_id VARCHAR NOT NULL,
    negotiation_claims   JSON
);

COMMENT ON COLUMN edc_contract_negotiation.agreement_id IS 'ContractAgreement serialized as JSON';
COMMENT ON COLUMN edc_contract_negotiation.contract_offers IS 'List<ContractOffer> serialized as JSON';
COMMENT ON COLUMN edc_contract_negotiation.trace_context IS 'Map<String,String> serialized as JSON';
COMMENT ON COLUMN edc_contract_negotiation.negotiation_claims IS 'Claims of the counter-party (Map<String,Object>) serialized as JSON';

CREATE INDEX IF NOT EXISTS contract_negotiation_correlationid_index
    ON edc_contract_negotiation (correlation_id);

CREATE INDEX IF NOT EXISTS contract_negotiation_agreement_id_index
    ON edc_contract_negotiation (agreement_id);

-- This will help to identify states that need to be transitioned without a table scan when the entries grow
CREATE INDEX IF NOT EXISTS contract_negotiation_state ON edc_contract_negotiation (state,state_timestamp);

-- Supports participant-scoped queries (e.g. management API listing), optionally sorted by creation date
CREATE INDEX IF NOT EXISTS contract_negotiation_participant_context_id_created_at_index
    ON edc_contract_negotiation (participant_context_id, created_at);

-- Supports participant-scoped agreement queries (e.g. management API listing)
CREATE INDEX IF NOT EXISTS contract_agreement_participant_context_id_index
    ON edc_contract_agreement (agr_participant_context_id, signing_date);

-- Supports the lookup of agreements by asset (e.g. check before asset deletion)
CREATE INDEX IF NOT EXISTS contract_agreement_asset_id_index
    ON edc_contract_agreement (asset_id);
