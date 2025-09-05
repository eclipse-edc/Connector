-- Statements are designed for and tested with Postgres only!

CREATE TABLE IF NOT EXISTS edc_lease
(
    leased_by         VARCHAR NOT NULL,
    leased_at         BIGINT,
    lease_duration    INTEGER NOT NULL,
    resource_id       VARCHAR NOT NULL,
    resource_kind     VARCHAR NOT NULL,
    PRIMARY KEY(resource_id, resource_kind)
);

COMMENT ON COLUMN edc_lease.leased_at IS 'posix timestamp of lease';
COMMENT ON COLUMN edc_lease.lease_duration IS 'duration of lease in milliseconds';

CREATE TABLE IF NOT EXISTS edc_policy_monitor
(
    entry_id             VARCHAR NOT NULL PRIMARY KEY,
    state                INTEGER NOT NULL            ,
    created_at           BIGINT  NOT NULL            ,
    updated_at           BIGINT  NOT NULL            ,
    state_count          INTEGER DEFAULT 0 NOT NULL,
    state_time_stamp     BIGINT,
    trace_context        JSON,
    error_detail         VARCHAR,
    properties           JSON,
    contract_id          VARCHAR
);

-- This will help to identify states that need to be transitioned without a table scan when the entries grow
CREATE INDEX IF NOT EXISTS policy_monitor_state ON edc_policy_monitor (state,state_time_stamp);