-- Statements are designed for and tested with Postgres only!

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
    contract_id          VARCHAR,
    leased_by            VARCHAR,
    leased_at            BIGINT,
    lease_duration       INTEGER
);

-- Add lease columns to tables created before the lease was moved from edc_lease to the entry row
ALTER TABLE edc_policy_monitor ADD COLUMN IF NOT EXISTS leased_by VARCHAR;
ALTER TABLE edc_policy_monitor ADD COLUMN IF NOT EXISTS leased_at BIGINT;
ALTER TABLE edc_policy_monitor ADD COLUMN IF NOT EXISTS lease_duration INTEGER;

COMMENT ON COLUMN edc_policy_monitor.leased_at IS 'posix timestamp of lease';
COMMENT ON COLUMN edc_policy_monitor.lease_duration IS 'duration of lease in milliseconds';

-- This will help to identify states that need to be transitioned without a table scan when the entries grow
CREATE INDEX IF NOT EXISTS policy_monitor_state ON edc_policy_monitor (state,state_time_stamp);