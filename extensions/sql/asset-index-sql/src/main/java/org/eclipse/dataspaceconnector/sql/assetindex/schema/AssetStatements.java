/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.sql.assetindex.schema;

import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.sql.translation.SqlQueryStatement;

/**
 * Defines queries used by the SqlAssetIndexServiceExtension.
 */
public interface AssetStatements {

    /**
     * The asset table name.
     */
    default String getAssetTable() {
        return "edc_asset";
    }

    /**
     * The asset table ID column.
     */
    default String getAssetIdColumn() {
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

    default String getDataAddressAssetIdFkColumn() {
        return "asset_id_fk";
    }

    default String getPropertyAssetIdFkColumn() {
        return "asset_id_fk";
    }

    /**
     * INSERT clause for assets.
     */
    String getInsertAssetTemplate();

    /**
     * INSERT clause for data addresses.
     */
    String getInsertDataAddressTemplate();

    /**
     * INSERT clause for properties.
     */
    String getInsertPropertyTemplate();

    /**
     * SELECT COUNT clause for assets.
     */
    String getCountAssetByIdClause();

    /**
     * SELECT clause for properties.
     */
    String getFindPropertyByIdTemplate();

    /**
     * SELECT clause for data addresses.
     */
    String getFindDataAddressByIdTemplate();

    /**
     * SELECT clause for all assets.
     */
    String getSelectAssetTemplate();

    /**
     * DELETE clause for assets.
     */
    String getDeleteAssetByIdTemplate();

    /**
     * The COUNT variable used in SELECT COUNT queries.
     */
    String getCountVariableName();

    /**
     * Provides a dynamically assembled SELECT statement for use with
     * {@link org.eclipse.dataspaceconnector.spi.query.QuerySpec} queries.
     */
    String getQuerySubSelectTemplate();

    /**
     * Operator to format an incoming string as JSON. Should return an empty string if the database does not support
     * this.
     */
    String getFormatAsJsonOperator();

    /**
     * Generates a SQL query using sub-select statements out of the query spec.
     *
     * @return A {@link SqlQueryStatement} that contains the SQL and statement parameters
     */
    SqlQueryStatement createQuery(QuerySpec query);
}
