-- Statements are designed for and tested with Postgres only!

CREATE TABLE IF NOT EXISTS edc_data_plane_instance
(
    id                          VARCHAR NOT NULL PRIMARY KEY,
    state                       INTEGER NOT NULL,
    state_time_stamp            BIGINT,
    created_at                  BIGINT  NOT NULL,
    updated_at                  BIGINT  NOT NULL,
    url                         VARCHAR NOT NULL,
    last_active                 BIGINT,
    participant_context_id      VARCHAR,
    allowed_source_types        JSON DEFAULT '[]',
    allowed_transfer_types      JSON DEFAULT '[]',
    destination_provision_types JSON DEFAULT '[]',
    labels                      JSON DEFAULT '[]',
    properties                  JSON DEFAULT '{}',
    authorization_profile       JSON
);

COMMENT ON COLUMN edc_data_plane_instance.allowed_source_types IS 'Java Set<String> serialized as JSON array';
COMMENT ON COLUMN edc_data_plane_instance.allowed_transfer_types IS 'Java Set<String> serialized as JSON array';
COMMENT ON COLUMN edc_data_plane_instance.destination_provision_types IS 'Java Set<String> serialized as JSON array';
COMMENT ON COLUMN edc_data_plane_instance.labels IS 'Java Set<String> serialized as JSON array';
COMMENT ON COLUMN edc_data_plane_instance.properties IS 'Java Map<String, Object> serialized as JSON';
COMMENT ON COLUMN edc_data_plane_instance.authorization_profile IS 'AuthorizationProfile serialized as JSON';

CREATE INDEX IF NOT EXISTS data_plane_instance_participant_context_id ON edc_data_plane_instance (participant_context_id);
CREATE INDEX IF NOT EXISTS data_plane_instance_state ON edc_data_plane_instance (state);
