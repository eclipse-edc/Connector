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

-- table: edc_cached_document
CREATE TABLE IF NOT EXISTS edc_cached_document
(
    id            VARCHAR NOT NULL,
    url           VARCHAR NOT NULL,
    content       JSON,
    document_type VARCHAR NOT NULL,
    pull_strategy VARCHAR NOT NULL,
    created_at    BIGINT  NOT NULL,
    updated_at    BIGINT  NOT NULL,
    PRIMARY KEY (id)
);

-- migration for deployments that created the table before the document_type column was introduced
ALTER TABLE edc_cached_document
    ADD COLUMN IF NOT EXISTS document_type VARCHAR NOT NULL DEFAULT 'JSON_LD';

COMMENT ON COLUMN edc_cached_document.content IS 'The cached document (a JSON-LD context or a JSON schema), serialized as JSON';

COMMENT ON COLUMN edc_cached_document.document_type IS 'The kind of cached document: JSON_LD or JSON_SCHEMA';

CREATE UNIQUE INDEX IF NOT EXISTS edc_cached_document_url_uindex
    ON edc_cached_document (url);
