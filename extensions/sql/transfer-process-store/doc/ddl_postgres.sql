-- Statements are designed for and tested with Postgres only!

DROP TABLE IF EXISTS edc_lease;
CREATE TABLE edc_lease
(
    leased_by      VARCHAR NOT NULL,
    leased_at      BIGINT,
    lease_duration INTEGER NOT NULL,
    lease_id       VARCHAR NOT NULL
        CONSTRAINT lease_pk
            PRIMARY KEY
);

COMMENT ON COLUMN edc_lease.leased_at IS 'posix timestamp of lease';

COMMENT ON COLUMN edc_lease.lease_duration IS 'duration of lease in milliseconds';

DROP TABLE IF EXISTS edc_transfer_process;
CREATE TABLE IF NOT EXISTS edc_transfer_process
(
    id                       VARCHAR           NOT NULL
        CONSTRAINT transfer_process_pk
            PRIMARY KEY,
    type                     VARCHAR           NOT NULL,
    state                    INTEGER           NOT NULL,
    state_count              INTEGER DEFAULT 0 NOT NULL,
    state_time_stamp         BIGINT,
    trace_context            VARCHAR,
    error_detail             VARCHAR,
    resource_manifest        VARCHAR,
    provisioned_resource_set VARCHAR,
    lease_id                 VARCHAR
        CONSTRAINT transfer_process_lease_lease_id_fk
            REFERENCES edc_lease
            ON DELETE SET NULL
);

COMMENT ON COLUMN edc_transfer_process.trace_context IS 'Java Map serialized as JSON';

COMMENT ON COLUMN edc_transfer_process.resource_manifest IS 'java ResourceManifest serialized as JSON';

COMMENT ON COLUMN edc_transfer_process.provisioned_resource_set IS 'ProvisionedResourceSet serialized as JSON';

CREATE UNIQUE INDEX transfer_process_id_uindex
    ON edc_transfer_process (id);

DROP TABLE IF EXISTS edc_data_request;
CREATE TABLE IF NOT EXISTS edc_data_request
(
    id                  VARCHAR NOT NULL
        CONSTRAINT data_request_pk
            PRIMARY KEY,
    process_id          VARCHAR NOT NULL,
    connector_address   VARCHAR NOT NULL,
    protocol            VARCHAR NOT NULL,
    connector_id        VARCHAR,
    asset_id            VARCHAR NOT NULL,
    contract_id         VARCHAR NOT NULL,
    data_destination    VARCHAR NOT NULL,
    managed_resources   BOOLEAN DEFAULT TRUE,
    properties          VARCHAR,
    transfer_type       VARCHAR,
    transfer_process_id VARCHAR NOT NULL
        CONSTRAINT data_request_transfer_process_id_fk
            REFERENCES edc_transfer_process
            ON UPDATE RESTRICT ON DELETE CASCADE
);

COMMENT ON COLUMN edc_data_request.data_destination IS 'DataAddress serialized as JSON';

COMMENT ON COLUMN edc_data_request.properties IS 'java Map serialized as JSON';

COMMENT ON COLUMN edc_data_request.transfer_type IS 'TransferType serialized as JSON';


CREATE UNIQUE INDEX data_request_id_uindex
    ON edc_data_request (id);

CREATE UNIQUE INDEX IF NOT EXISTS lease_lease_id_uindex
    ON edc_lease (lease_id);

