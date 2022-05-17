--
--  Copyright (c) 2022 ZF Friedrichshafen AG
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--       ZF Friedrichshafen AG - Initial SQL Query
--

-- Statements are designed for and tested with Postgres only!

-- table: edc_policies
CREATE TABLE IF NOT EXISTS edc_policies
(
    policy_id VARCHAR NOT NULL,
    permissions VARCHAR,
    prohibitions VARCHAR,
    duties VARCHAR,
    extensible_properties VARCHAR,
    inherits_from VARCHAR,
    assigner VARCHAR,
    assignee VARCHAR,
    target VARCHAR,
    policy_type VARCHAR NOT NULL,
    PRIMARY KEY (policy_id)
);

COMMENT ON COLUMN edc_policies.permissions IS 'Java List<Permission> serialized as JSON';
COMMENT ON COLUMN edc_policies.prohibitions IS 'Java List<Prohibition> serialized as JSON';
COMMENT ON COLUMN edc_policies.duties IS 'Java List<Duty> serialized as JSON';
COMMENT ON COLUMN edc_policies.extensible_properties IS 'Java Map<String, Object> serialized as JSON';
COMMENT ON COLUMN edc_policies.policy_type IS 'Java PolicyType serialized as JSON';

CREATE UNIQUE INDEX IF NOT EXISTS edc_policies_id_uindex
    ON edc_policies (policy_id);
