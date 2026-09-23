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

-- only intended for and tested with Postgres!
CREATE TABLE IF NOT EXISTS edc_partner_group
(
    participant_context_id VARCHAR NOT NULL,        -- ID of the owning ParticipantContext
    id                     VARCHAR NOT NULL,        -- ID of the group, unique within the participant context
    name                   VARCHAR NOT NULL,
    description            VARCHAR,
    properties             JSON DEFAULT '{}',       -- Java Map<String,Object> serialized as JSON
    created_at             BIGINT  NOT NULL,        -- POSIX timestamp of the creation
    PRIMARY KEY (participant_context_id, id)
);

CREATE TABLE IF NOT EXISTS edc_partner
(
    participant_context_id VARCHAR NOT NULL,        -- ID of the owning ParticipantContext
    id                     VARCHAR NOT NULL,        -- ID of the partner, unique within the participant context
    identity               VARCHAR NOT NULL,        -- counterparty identity (e.g. DID), unique within the participant context
    name                   VARCHAR,
    properties             JSON DEFAULT '{}',       -- Java Map<String,Object> serialized as JSON
    group_ids              JSON DEFAULT '[]',       -- Java Set<String> of PartnerGroup ids serialized as JSON array
    created_at             BIGINT  NOT NULL,        -- POSIX timestamp of the creation
    PRIMARY KEY (participant_context_id, id),
    CONSTRAINT edc_partner_identity_uq UNIQUE (participant_context_id, identity)
);
