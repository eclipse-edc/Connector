--
--  Copyright (c) 2026 Metaform Systems, Inc.
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--       Metaform Systems, Inc. - initial SQL schema
--

-- Statements are designed for and tested with Postgres only!

-- table: edc_schema_validator_registration
CREATE TABLE IF NOT EXISTS edc_schema_validator_registration
(
    id             VARCHAR NOT NULL,
    version        VARCHAR NOT NULL,
    validated_type VARCHAR NOT NULL,
    schema_id      VARCHAR NOT NULL,
    profiles       JSON,
    created_at     BIGINT  NOT NULL,
    updated_at     BIGINT  NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS edc_schema_validator_registration_version_type_index
    ON edc_schema_validator_registration (version, validated_type);
