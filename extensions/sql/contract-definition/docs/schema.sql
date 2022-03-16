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

-- table: edc_contract_definitions
CREATE TABLE IF NOT EXISTS edc_contract_definitions
(
    contract_definition_id VARCHAR(255) NOT NULL,
    access_policy VARCHAR(MAX) NOT NULL,
    contract_policy VARCHAR(MAX) NOT NULL,
    selector_expression VARCHAR(MAX) NOT NULL,
    PRIMARY KEY (contract_definition_id)
);
