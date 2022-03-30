--
--  Copyright (c) 2022 ZF Friedrichshafen AG - Initial API and Implementation
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--       ZF Friedrichshafen AG - Initial API and Implementation
--

-- table: edc_policies
CREATE TABLE IF NOT EXISTS edc_policies
(
    policy_id VARCHAR(255) NOT NULL,
    permissions VARCHAR(10000),
    prohibitions VARCHAR(10000),
    duties VARCHAR(10000),
    extensible_properties VARCHAR(10000),
    inherits_from VARCHAR(10000),
    assigner VARCHAR(10000),
    assignee VARCHAR(10000),
    target VARCHAR(10000),
    policy_type VARCHAR(10000) NOT NULL,
    PRIMARY KEY (policy_id)
);
