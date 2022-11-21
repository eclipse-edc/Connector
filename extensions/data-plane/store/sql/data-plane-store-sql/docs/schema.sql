CREATE TABLE IF NOT EXISTS edc_data_plane
(
    process_id           VARCHAR NOT NULL PRIMARY KEY,
    state                INTEGER NOT NULL            ,
    created_at           BIGINT  NOT NULL            ,
    updated_at           BIGINT  NOT NULL
);
