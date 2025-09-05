-- Statements are designed for and tested with Postgres only!

CREATE TABLE IF NOT EXISTS edc_lease
(
    leased_by      VARCHAR NOT NULL,
    leased_at      BIGINT,
    lease_duration INTEGER NOT NULL,
    resource_id       VARCHAR NOT NULL,
    resource_kind  VARCHAR NOT NULL,
    PRIMARY KEY(resource_id, resource_kind)
);

COMMENT ON COLUMN edc_lease.leased_at IS 'posix timestamp of lease';

COMMENT ON COLUMN edc_lease.lease_duration IS 'duration of lease in milliseconds';


-- test entity
CREATE TABLE IF NOT EXISTS edc_test_entity
(
    id       VARCHAR NOT NULL CONSTRAINT test_id_pk PRIMARY KEY
);

CREATE UNIQUE INDEX IF NOT EXISTS test_entity_id_uindex
    ON edc_test_entity (id);