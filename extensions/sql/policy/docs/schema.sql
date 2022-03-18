--
--  Copyright (c) 2022 Daimler TSS GmbH
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--       Daimler TSS GmbH - Initial SQL Query
--

-- table: edc_policies
CREATE TABLE IF NOT EXISTS edc_policies
(
    policy_id VARCHAR(255) NOT NULL,
    permissions VARCHAR(MAX),
    prohibitions VARCHAR(MAX),
    duties VARCHAR(MAX),
    extensible_properties VARCHAR(MAX),
    inherits_from VARCHAR(MAX),
    assigner VARCHAR(MAX),
    assignee VARCHAR(MAX),
    target VARCHAR(MAX),
    policy_type VARCHAR(MAX) NOT NULL,
    PRIMARY KEY (policy_id)
);
