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
CREATE TABLE IF NOT EXISTS participant_context
(
    participant_context_id VARCHAR PRIMARY KEY NOT NULL, -- ID of the ParticipantContext
    created_date           BIGINT              NOT NULL, -- POSIX timestamp of the creation of the PC
    last_modified_date     BIGINT,                       -- POSIX timestamp of the last modified date
    state                  INTEGER             NOT NULL, -- 0 = CREATED, 1 = ACTIVE, 2 = DEACTIVATED
    properties             JSON DEFAULT '{}'             -- JSON object containing additional information, such as OAuth2 client secret aliases
);
CREATE UNIQUE INDEX IF NOT EXISTS participant_context_participant_context_id_uindex ON participant_context USING btree (participant_context_id);

