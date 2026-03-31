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
CREATE TABLE IF NOT EXISTS edc_tasks
(
    id                      VARCHAR PRIMARY KEY NOT NULL,
    name                    VARCHAR             NOT NULL,
    task_group              VARCHAR             NOT NULL,
    payload                 JSON DEFAULT '{}',
    retry_count             INT                 NOT NULL,
    timestamp               BIGINT              NOT NULL
);

