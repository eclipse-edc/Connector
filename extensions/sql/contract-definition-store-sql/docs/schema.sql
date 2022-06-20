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
--       Microsoft Corporation - refactoring
--

-- table: edc_contract_definitions
-- only intended for and tested with H2 and Postgres!
CREATE TABLE IF NOT EXISTS edc_contract_definitions
(
    contract_definition_id VARCHAR NOT NULL,
    access_policy_id       VARCHAR NOT NULL,
    contract_policy_id     VARCHAR NOT NULL,
    selector_expression    JSON NOT NULL,
    PRIMARY KEY (contract_definition_id)
);
