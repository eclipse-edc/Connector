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
    String getAssetTable();

    /**
     * The asset table ID column.
     */
    String getAssetColumnId();

    /**
     * The data address table name.
     */
    String getDataAddressTable();

    /**
     * The data address table properties column.
     */
    String getDataAddressColumnProperties();

    /**
     * The asset property table name.
     */
    String getAssetPropertyTable();

    /**
     * The asset property name column.
     */
    String getAssetPropertyColumnName();

    /**
     * The asset property value column.
     */
    String getAssetPropertyColumnValue();

    /**
     * The asset property type column.
     */
    String getAssetPropertyColumnType();
}
