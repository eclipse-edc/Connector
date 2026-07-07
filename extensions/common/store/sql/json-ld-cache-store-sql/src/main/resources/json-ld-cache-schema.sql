--
--  Copyright (c) 2026 2026 Metaform Systems, Inc.
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

-- table: edc_json_ld_context_cache
CREATE TABLE IF NOT EXISTS edc_json_ld_context_cache
(
    id            VARCHAR NOT NULL,
    url           VARCHAR NOT NULL,
    content       JSON,
    pull_strategy VARCHAR NOT NULL,
    created_at    BIGINT  NOT NULL,
    updated_at    BIGINT  NOT NULL,
    PRIMARY KEY (id)
);

COMMENT ON COLUMN edc_json_ld_context_cache.content IS 'The cached JSON-LD context document, serialized as JSON';

CREATE UNIQUE INDEX IF NOT EXISTS edc_json_ld_context_cache_url_uindex
    ON edc_json_ld_context_cache (url);
