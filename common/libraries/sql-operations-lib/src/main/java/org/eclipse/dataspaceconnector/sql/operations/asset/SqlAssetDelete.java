/*
 *  Copyright (c) 2021 Daimler TSS GmbH
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

package org.eclipse.dataspaceconnector.sql.operations.asset;

import org.eclipse.dataspaceconnector.sql.operations.util.PreparedStatementResourceReader;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

public class SqlAssetDelete {

    private final Connection connection;

    public SqlAssetDelete(@NotNull Connection connection) {
        this.connection = Objects.requireNonNull(connection);
    }

    /**
     * Delete an Asset.
     *
     * @param assetId id of the asset to delete
     * @return number of rows changed
     * @throws SQLException if execution of the query was failing
     */
    public int execute(@NotNull String assetId) throws SQLException {
        Objects.requireNonNull(assetId);

        String sqlDelete = PreparedStatementResourceReader.readAssetDelete();
        return executeQuery(connection, sqlDelete, assetId);
    }
}
