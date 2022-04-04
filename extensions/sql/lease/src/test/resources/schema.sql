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

CREATE UNIQUE INDEX IF NOT EXISTS lease_lease_id_uindex
    ON edc_lease (lease_id);


-- test entity
DROP TABLE IF EXISTS edc_test_entity;
CREATE TABLE edc_test_entity
(
    id       VARCHAR NOT NULL
        CONSTRAINT test_id_pk PRIMARY KEY,
    lease_id VARCHAR
        CONSTRAINT test_entity_lease_id_fk
            REFERENCES edc_lease
            ON DELETE SET NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS test_entity_id_uindex
    ON edc_test_entity (id);