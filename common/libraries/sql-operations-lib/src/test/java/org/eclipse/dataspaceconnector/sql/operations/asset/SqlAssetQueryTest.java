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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SqlDataSourceExtension.class)
public class SqlAssetQueryTest {

    private DataSource dataSource;

    @BeforeEach
    public void setup(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Test
    public void testSelectAllQuery() throws SQLException {
        Asset asset1 = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();

        Asset asset2 = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();

        Connection connection = dataSource.getConnection();
        SqlAssetInsert insert = new SqlAssetInsert(connection);
        insert.execute(asset1);
        insert.execute(asset2);

        // TODO Wait until its verified how an select all looks like

        SqlAssetQuery sqlAssetQuery = new SqlAssetQuery(connection);
        List<Asset> assets = sqlAssetQuery.execute();

        assertThat(assets.stream().map(Asset::getId).collect(Collectors.toUnmodifiableList()))
                .contains(asset1.getId())
                .contains(asset2.getId());
    }

    @Test
    public void testSelectById() throws SQLException {
        Asset asset1 = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();

        Asset asset2 = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();

        Connection connection = dataSource.getConnection();
        SqlAssetInsert insert = new SqlAssetInsert(connection);
        insert.execute(asset1);
        insert.execute(asset2);

        Map<String, Object> filters = Collections.singletonMap(Asset.PROPERTY_ID, asset1.getId());
        SqlAssetQuery sqlAssetQuery = new SqlAssetQuery(connection);
        List<Asset> assets = sqlAssetQuery.execute(filters);

        assertThat(assets.stream().map(Asset::getId).collect(Collectors.toUnmodifiableList()))
                .contains(asset1.getId());
    }

    @Test
    public void testSelectMultiple() throws SQLException {

        Asset asset1 = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contentType("pdf")
                .version("1.0.0")
                .build();

        Asset asset2 = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contentType("pdf")
                .version("1.0.0")
                .build();

        Asset asset3 = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contentType("pdf")
                .version("1.0.1")
                .build();

        Connection connection = dataSource.getConnection();
        SqlAssetInsert insert = new SqlAssetInsert(connection);
        insert.execute(asset1);
        insert.execute(asset2);
        insert.execute(asset3);

        Map<String, Object> filters = new HashMap<>();
        filters.put(Asset.PROPERTY_CONTENT_TYPE, "pdf");
        filters.put(Asset.PROPERTY_VERSION, "1.0.0");

        SqlAssetQuery sqlAssetQuery = new SqlAssetQuery(connection);
        List<Asset> assets = sqlAssetQuery.execute(filters);

        assertThat(assets).size().isEqualTo(2);
        assertThat(assets.stream().map(Asset::getId).collect(Collectors.toUnmodifiableList()))
                .contains(asset1.getId())
                .contains(asset2.getId());
    }
}
