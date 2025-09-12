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
 *       ZF Friedrichshafen AG - added private property support
 *
 */

package org.eclipse.edc.connector.controlplane.store.sql.assetindex.schema;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.statement.SqlStatements;
import org.eclipse.edc.sql.translation.SqlQueryStatement;

import java.util.List;

/**
 * Defines queries used by the SqlAssetIndexServiceExtension.
 */
@ExtensionPoint
public interface AssetStatements extends SqlStatements {

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

    default String getPropertiesColumn() {
        return "properties";
    }

    default String getPrivatePropertiesColumn() {
        return "private_properties";
    }

    default String getDataAddressColumn() {
        return "data_address";
    }

    default String getParticipantContextIdColumn() {
        return "participant_context_id";
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
    default String getDataAddressPropertiesColumn() {
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
    default String getAssetPropertyNameColumn() {
        return "property_name";
    }

    /**
     * The asset property value column.
     */
    default String getAssetPropertyValueColumn() {
        return "property_value";
    }

    /**
     * The asset property type column.
     */
    default String getAssetPropertyTypeColumn() {
        return "property_type";
    }

    /**
     * The asset property is private column.
     */
    default String getAssetPropertyIsPrivateColumn() {
        return "property_is_private";
    }

    default String getDataAddressAssetIdFkColumn() {
        return "asset_id_fk";
    }

    default String getPropertyAssetIdFkColumn() {
        return "asset_id_fk";
    }

    default String getCreatedAtColumn() {
        return "created_at";
    }

    /**
     * INSERT clause for assets.
     */
    String getInsertAssetTemplate();

    /**
     * UPDATE clause for assets.
     */
    String getUpdateAssetTemplate();

    /**
     * SELECT COUNT clause for assets.
     */
    String getCountAssetByIdClause();

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
     * Generates a SQL query using sub-select statements out of the query spec.
     *
     * @return A {@link SqlQueryStatement} that contains the SQL and statement parameters
     */
    SqlQueryStatement createQuery(QuerySpec query);

    /**
     * Generates a SQL query using sub-select statements out of the criterion.
     *
     * @return A {@link SqlQueryStatement} that contains the SQL and statement parameters
     */
    SqlQueryStatement createQuery(List<Criterion> query);

}
