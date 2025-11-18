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
CREATE TABLE IF NOT EXISTS edc_participant_context_config
(
    participant_context_id VARCHAR PRIMARY KEY NOT NULL, -- ID of the ParticipantContext
    created_date           BIGINT              NOT NULL, -- POSIX timestamp of the creation of the PC
    last_modified_date     BIGINT,                       -- POSIX timestamp of the last modified date
    entries                 JSON DEFAULT '{}'             -- JSON object containing the configuration data
);

