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

package org.eclipse.dataspaceconnector.sql.asset.index;

/**
 * Defines queries used by the SqlAssetIndexServiceExtension.
 */
public interface SqlAssetQueries extends SqlAssetTables {

    /**
     * INSERT clause for assets.
     */
    String getSqlAssetInsertClause();

    /**
     * INSERT clause for data addresses.
     */
    String getSqlDataAddressInsertClause();

    /**
     * INSERT clause for properties.
     */
    String getSqlPropertyInsertClause();

    /**
     * SELECT COUNT clause for assets.
     */
    String getSqlAssetCountByIdClause();

    /**
     * SELECT clause for properties.
     */
    String getSqlPropertyFindByIdClause();

    /**
     * SELECT clause for data addresses.
     */
    String getSqlDataAddressFindByIdClause();

    /**
     * SELECT clause for all assets.
     */
    String getSqlAssetListClause();

    /**
     * DELETE clause for assets.
     */
    String getSqlAssetDeleteByIdClause();

    /**
     * DELETE clause for data addresses.
     */
    String getSqlDataAddressDeleteByIdClause();

    /**
     * DELETE clause for properties.
     */
    String getSqlPropertyDeleteByIdClause();

    /**
     * The COUNT variable used in SELECT COUNT queries.
     */
    String getCountVariableName();

    /**
     * Provides a dynamically assembled SELECT statement for use with {@link org.eclipse.dataspaceconnector.spi.query.QuerySpec}
     * queries.
     */
    String getQuerySubSelectClause();
}
