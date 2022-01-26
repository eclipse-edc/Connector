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

import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.sql.operations.SqlDataSourceExtension;
import org.eclipse.dataspaceconnector.sql.operations.mapper.ExistsMapper;
import org.eclipse.dataspaceconnector.sql.operations.mapper.PropertyMapper;
import org.eclipse.dataspaceconnector.sql.operations.types.Property;
import org.eclipse.dataspaceconnector.sql.operations.util.PreparedStatementResourceReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

@ExtendWith(SqlDataSourceExtension.class)
public class SqlAssetDeleteTest {

    private DataSource dataSource;

    @BeforeEach
    public void setup(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Test
    public void testAssetDelete() throws SQLException {
        Asset asset = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contentType("pdf")
                .version("1.0.0")
                .build();

        Connection connection = dataSource.getConnection();

        SqlAssetInsert insert = new SqlAssetInsert(connection);
        insert.execute(asset);

        SqlAssetDelete delete = new SqlAssetDelete(connection);
        int rowsDeleted = delete.execute(asset.getId());

        List<Property> properties = executeQuery(connection, new PropertyMapper(),
                PreparedStatementResourceReader.readPropertiesSelectByAssetId(), asset.getId());
        List<Boolean> exists = executeQuery(connection, new ExistsMapper(),
                PreparedStatementResourceReader.readAssetExists(), asset.getId());

        Assertions.assertTrue(rowsDeleted > 0);
        Assertions.assertFalse(exists.size() == 1 && exists.get(0));
        Assertions.assertEquals(0, properties.size());
    }

    @Test
    public void testNonExistingAssetDelete() throws SQLException {
        Asset asset = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contentType("pdf")
                .version("1.0.0")
                .build();

        Connection connection = dataSource.getConnection();

        SqlAssetInsert insert = new SqlAssetInsert(connection);
        insert.execute(asset);

        SqlAssetDelete delete = new SqlAssetDelete(connection);
        int rowsDeleted = delete.execute(asset.getId() + "doesnt_exist");

        Assertions.assertEquals(0, rowsDeleted);
    }
}
