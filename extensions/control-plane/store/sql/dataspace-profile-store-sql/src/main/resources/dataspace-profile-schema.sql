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
--       Metaform Systems, Inc. - initial API and implementation
--

-- Statements are designed for and tested with Postgres only!

-- table: edc_dataspace_profiles
CREATE TABLE IF NOT EXISTS edc_dataspace_profiles
(
    name                VARCHAR NOT NULL,
    protocol_version    VARCHAR,
    path                VARCHAR,
    binding             VARCHAR,
    namespace           VARCHAR,
    jsonld_contexts_url JSON,
    created_at          BIGINT  NOT NULL,
    PRIMARY KEY (name)
);

COMMENT ON COLUMN edc_dataspace_profiles.jsonld_contexts_url IS 'Java List<String> serialized as JSON';
