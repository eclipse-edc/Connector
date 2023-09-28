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
--       SAP SE - add private properties to contract definition
--

-- table: edc_contract_definitions
-- only intended for and tested with H2 and Postgres!
CREATE TABLE IF NOT EXISTS edc_contract_definitions
(
    created_at             BIGINT  NOT NULL,
    contract_definition_id VARCHAR NOT NULL,
    access_policy_id       VARCHAR NOT NULL,
    contract_policy_id     VARCHAR NOT NULL,
    assets_selector        JSON    NOT NULL,
    PRIMARY KEY (contract_definition_id)
);


-- table: edc_contract_definition_property
CREATE TABLE IF NOT EXISTS edc_contract_definition_property
(
    contract_definition_id_fk       VARCHAR(255) NOT NULL,
    property_name  VARCHAR(255) NOT NULL,
    property_value TEXT         NOT NULL,
    property_type  VARCHAR(255) NOT NULL,
    property_is_private BOOLEAN,
    PRIMARY KEY (contract_definition_id_fk, property_name),
    FOREIGN KEY (contract_definition_id_fk) REFERENCES edc_contract_definitions (contract_definition_id) ON DELETE CASCADE
);


CREATE INDEX IF NOT EXISTS idx_edc_contract_definition_property_value
    ON edc_contract_definition_property (property_name, property_value);


COMMENT ON COLUMN edc_contract_definition_property.property_name IS
    'Contract definition property key';
COMMENT ON COLUMN edc_contract_definition_property.property_value IS
    'Contract definition property value';
COMMENT ON COLUMN edc_contract_definition_property.property_type IS
    'Contract definition property class name';
COMMENT ON COLUMN edc_contract_definition_property.property_is_private IS
    'Contract definition property private flag';
