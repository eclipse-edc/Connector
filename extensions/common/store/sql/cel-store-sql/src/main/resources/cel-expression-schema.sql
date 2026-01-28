/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

-- only intended for and tested with Postgres!
CREATE TABLE IF NOT EXISTS edc_cel_expression
(
    id VARCHAR PRIMARY KEY NOT NULL, -- ID
    scopes                 JSON DEFAULT '{}',
    left_operand           TEXT NOT NULL,
    expression             TEXT NOT NULL,
    description            TEXT NOT NULL,
    created_date           BIGINT              NOT NULL, -- POSIX timestamp of the creation of the PC
    last_modified_date     BIGINT              NOT NULL       -- POSIX timestamp of the last modified date
);

