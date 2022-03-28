/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.sql.asset.loader;

public interface SqlAssetTables {
    String ASSET_TABLE = "edc_asset";
    String ASSET_COLUMN_ID = "asset_id";

    String DATA_ADDRESS_TABLE = "edc_asset_dataaddress";
    String DATA_ADDRESS_COLUMN_PROPERTIES = "properties";

    String ASSET_PROPERTY_TABLE = "edc_asset_property";
    String ASSET_PROPERTY_COLUMN_NAME = "property_name";
    String ASSET_PROPERTY_COLUMN_VALUE = "property_value";
    String ASSET_PROPERTY_COLUMN_TYPE = "property_type";
}
