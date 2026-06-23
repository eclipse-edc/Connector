/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

-- Statements are designed for and tested with Postgres only!

CREATE TABLE IF NOT EXISTS edc_dcp_scope
(
    id             VARCHAR NOT NULL PRIMARY KEY,
    type           VARCHAR NOT NULL,
    value          VARCHAR NOT NULL,
    profile        VARCHAR NOT NULL,
    prefix_mapping VARCHAR
);

COMMENT ON COLUMN edc_dcp_scope.type IS 'DcpScope type (DEFAULT or POLICY)';
COMMENT ON COLUMN edc_dcp_scope.value IS 'The value identifying the actual scope';
COMMENT ON COLUMN edc_dcp_scope.prefix_mapping IS 'Optional prefix mapping, required for POLICY type';
