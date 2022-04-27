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

package org.eclipse.dataspaceconnector.sql.asset.index;

/**
 * Defines table and column names used by the SqlAssetIndexServiceExtension.
 */
public interface SqlAssetTables {
    /**
     * The asset table name.
     */
    default String getAssetTable() {
        return "edc_asset";
    }

    /**
     * The asset table ID column.
     */
    default String getAssetColumnId() {
        return "asset_id";
    }

    /**
     * The data address table name.
     */
    default String getDataAddressTable() {
        return "edc_asset_dataaddress";
    }

    /**
     * The data address table properties column.
     */
    default String getDataAddressColumnProperties() {
        return "properties";
    }

    /**
     * The asset property table name.
     */
    default String getAssetPropertyTable() {
        return "edc_asset_property";
    }

    /**
     * The asset property name column.
     */
    default String getAssetPropertyColumnName() {
        return "property_name";
    }

    /**
     * The asset property value column.
     */
    default String getAssetPropertyColumnValue() {
        return "property_value";
    }

    /**
     * The asset property type column.
     */
    default String getAssetPropertyColumnType() {
        return "property_type";
    }
}
