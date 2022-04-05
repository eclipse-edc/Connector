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

-- THIS SCHEMA HAS BEEN WRITTEN AND TESTED ONLY FOR POSTGRES

-- table: edc_asset
CREATE TABLE IF NOT EXISTS edc_asset (
    asset_id VARCHAR NOT NULL,
    PRIMARY KEY (asset_id)
    );

-- table: edc_asset_dataaddress
CREATE TABLE IF NOT EXISTS edc_asset_dataaddress (
    asset_id VARCHAR NOT NULL,
    properties VARCHAR NOT NULL,
    PRIMARY KEY (asset_id),
    FOREIGN KEY (asset_id) REFERENCES edc_asset (asset_id) ON DELETE CASCADE
    );
COMMENT ON COLUMN edc_asset_dataaddress.properties is 'DataAddress properties serialized as JSON';

-- table: edc_asset_property
CREATE TABLE IF NOT EXISTS edc_asset_property (
    asset_id VARCHAR NOT NULL,
    property_name VARCHAR NOT NULL,
    property_value VARCHAR NOT NULL,
    property_type VARCHAR NOT NULL,
    PRIMARY KEY (asset_id, property_name),
    FOREIGN KEY (asset_id) REFERENCES edc_asset (asset_id) ON DELETE CASCADE
    );

COMMENT ON COLUMN edc_asset_property.property_name IS
    'Asset property key';
COMMENT ON COLUMN edc_asset_property.property_value IS
    'Asset property value';
COMMENT ON COLUMN edc_asset_property.property_type IS
    'Asset property class name';

CREATE INDEX IF NOT EXISTS idx_edc_asset_property_value
    ON edc_asset_property(property_name, property_value);