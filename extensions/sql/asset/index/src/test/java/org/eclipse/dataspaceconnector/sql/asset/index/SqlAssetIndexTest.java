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
 *       Daimler TSS GmbH - Initial Tests
 *
 */

package org.eclipse.dataspaceconnector.sql.asset.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.dataloading.AssetEntry;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.sql.SqlQueryExecutor;
import org.eclipse.dataspaceconnector.sql.datasource.ConnectionFactoryDataSource;
import org.eclipse.dataspaceconnector.sql.datasource.ConnectionPoolDataSource;
import org.eclipse.dataspaceconnector.sql.pool.ConnectionPool;
import org.eclipse.dataspaceconnector.sql.pool.commons.CommonsConnectionPool;
import org.eclipse.dataspaceconnector.sql.pool.commons.CommonsConnectionPoolConfig;
import org.eclipse.dataspaceconnector.transaction.local.DataSourceResource;
import org.eclipse.dataspaceconnector.transaction.local.LocalDataSourceRegistry;
import org.eclipse.dataspaceconnector.transaction.local.LocalTransactionContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

public class SqlAssetIndexTest {

    private static final String DATASOURCE_NAME = "asset";

    private SqlAssetIndex sqlAssetIndex;
    private ConnectionPool connectionPool;
    private DataSourceRegistry dataSourceRegistry;
    private PostgresSqlAssetQueries sqlAssetQueries;
    private TransactionContext transactionContext;

    @BeforeEach
    void setUp() throws SQLException {
        var monitor = new Monitor() {
        };
        var txManager = new LocalTransactionContext(monitor);
        dataSourceRegistry = new LocalDataSourceRegistry(txManager);
        transactionContext = txManager;
        var jdbcDataSource = new JdbcDataSource();
        jdbcDataSource.setURL("jdbc:h2:mem:");

        var connection = jdbcDataSource.getConnection();
        var dataSource = new ConnectionFactoryDataSource(() -> connection);
        connectionPool = new CommonsConnectionPool(dataSource, CommonsConnectionPoolConfig.Builder.newInstance().build());
        var poolDataSource = new ConnectionPoolDataSource(connectionPool);
        dataSourceRegistry.register(DATASOURCE_NAME, poolDataSource);
        txManager.registerResource(new DataSourceResource(poolDataSource));
        sqlAssetIndex = new SqlAssetIndex(dataSourceRegistry, DATASOURCE_NAME, transactionContext, new ObjectMapper(), new PostgresSqlAssetQueries());

        try (var inputStream = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            var schema = new String(Objects.requireNonNull(inputStream).readAllBytes(), StandardCharsets.UTF_8);
            transactionContext.execute(() -> SqlQueryExecutor.executeQuery(connection, schema));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        sqlAssetQueries = new PostgresSqlAssetQueries();
    }

    @AfterEach
    void tearDown() throws Exception {
        connectionPool.close();
    }

    @Test
    @DisplayName("Accept an asset and a data address that don't exist yet")
    void acceptAssetAndDataAddress_doesNotExist() {
        var assetExpected = getAsset("id1");
        sqlAssetIndex.accept(assetExpected, getDataAddress());

        try (var connection = getConnection()) {

            var assetProperties = transactionContext.execute(() -> {
                var assetCount = executeQuery(connection, sqlAssetIndex::mapRowCount, sqlAssetQueries.getSqlAssetCountByIdClause(), assetExpected.getId()).iterator().next();

                if (assetCount <= 0) {
                    return null;
                } else if (assetCount > 1) {
                    throw new IllegalStateException("Expected result set size of 0 or 1 but got " + assetCount);
                }

                return executeQuery(connection, sqlAssetIndex::mapPropertyResultSet, sqlAssetQueries.getSqlPropertyFindByIdClause(), assetExpected.getId()).stream().collect(Collectors.toMap(
                        AbstractMap.SimpleImmutableEntry::getKey,
                        AbstractMap.SimpleImmutableEntry::getValue));
            });

            var assetFound = Asset.Builder.newInstance().id(assetExpected.getId()).properties(assetProperties).build();

            assertThat(assetFound).isNotNull();
            assertThat(assetFound).usingRecursiveComparison().isEqualTo(assetExpected);

        } catch (Exception e) {
            if (e instanceof EdcPersistenceException) {
                throw (EdcPersistenceException) e;
            } else {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        }
    }

    @Test
    @DisplayName("Accept an asset and a data address that already exist")
    void acceptAssetAndDataAddress_exists() {
        var asset = getAsset("id1");
        sqlAssetIndex.accept(asset, getDataAddress());

        assertThatThrownBy(() -> sqlAssetIndex.accept(asset, getDataAddress()))
                .isInstanceOf(EdcPersistenceException.class)
                .hasMessageContaining(String.format("Cannot persist. Asset with ID '%s' already exists.", asset.getId()));
    }

    @Test
    @DisplayName("Accept an asset entry that doesn't exist yet")
    void acceptAssetEntry_doesNotExist() {
        var assetExpected = getAsset("id1");
        sqlAssetIndex.accept(new AssetEntry(assetExpected, getDataAddress()));

        try (var connection = getConnection()) {

            var assetProperties = transactionContext.execute(() -> {
                var assetCount = executeQuery(connection, sqlAssetIndex::mapRowCount, sqlAssetQueries.getSqlAssetCountByIdClause(), assetExpected.getId()).iterator().next();

                if (assetCount <= 0) {
                    return null;
                } else if (assetCount > 1) {
                    throw new IllegalStateException("Expected result set size of 0 or 1 but got " + assetCount);
                }

                return executeQuery(connection, sqlAssetIndex::mapPropertyResultSet, sqlAssetQueries.getSqlPropertyFindByIdClause(), assetExpected.getId()).stream().collect(Collectors.toMap(
                        AbstractMap.SimpleImmutableEntry::getKey,
                        AbstractMap.SimpleImmutableEntry::getValue));
            });

            var assetFound = Asset.Builder.newInstance().id(assetExpected.getId()).properties(assetProperties).build();

            assertThat(assetFound).isNotNull();
            assertThat(assetFound).usingRecursiveComparison().isEqualTo(assetExpected);

        } catch (Exception e) {
            if (e instanceof EdcPersistenceException) {
                throw (EdcPersistenceException) e;
            } else {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        }
    }

    @Test
    @DisplayName("Accept an asset entry that already exists")
    void acceptEntry_exists() {
        var asset = getAsset("id1");
        sqlAssetIndex.accept(new AssetEntry(asset, getDataAddress()));

        assertThatThrownBy(() -> sqlAssetIndex.accept(asset, getDataAddress()))
                .isInstanceOf(EdcPersistenceException.class)
                .hasMessageContaining(String.format("Cannot persist. Asset with ID '%s' already exists.", asset.getId()));
    }

    @Test
    @DisplayName("Delete an asset that doesn't exist")
    void deleteAsset_doesNotExist() {
        var assetDeleted = sqlAssetIndex.deleteById("id1");

        assertThat(assetDeleted).isNull();
    }

    @Test
    @DisplayName("Delete an asset that exists")
    void deleteAsset_exists() {
        var asset = getAsset("id1");
        sqlAssetIndex.accept(asset, getDataAddress());

        var assetDeleted = sqlAssetIndex.deleteById("id1");

        assertThat(assetDeleted).isNotNull();
        assertThat(assetDeleted).usingRecursiveComparison().isEqualTo(asset);

        try (var connection = getConnection()) {
            transactionContext.execute(() -> {
                var assetCount = executeQuery(connection, sqlAssetIndex::mapRowCount,
                        String.format("SELECT COUNT(*) AS %s FROM %s",
                                sqlAssetQueries.getCountVariableName(),
                                sqlAssetQueries.getAssetTable()
                        )).iterator().next();
                assertThat(assetCount).isEqualTo(0);
            });
        } catch (Exception e) {
            if (e instanceof EdcPersistenceException) {
                throw (EdcPersistenceException) e;
            } else {
                throw new EdcPersistenceException(e.getMessage(), e);
            }
        }
    }

    @Test
    @DisplayName("Query assets with selector expression")
    void queryAsset_selectorExpression() {
        var asset1 = getAsset("id1");
        sqlAssetIndex.accept(asset1, getDataAddress());
        var asset2 = getAsset("id2");
        sqlAssetIndex.accept(asset2, getDataAddress());

        var assetsFound = sqlAssetIndex.queryAssets(AssetSelectorExpression.Builder.newInstance().build());

        assertThat(assetsFound).isNotNull();
        assertThat(assetsFound.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Query assets with query spec")
    void queryAsset_querySpec() {
        for (var i = 1; i <= 10; i++) {
            var asset = getAsset("id" + i);
            sqlAssetIndex.accept(asset, getDataAddress());
        }

        var assetsFound = sqlAssetIndex.queryAssets(getQuerySpec());

        assertThat(assetsFound).isNotNull();
        assertThat(assetsFound.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("Query assets with query spec and short asset count")
    void queryAsset_querySpecShortCount() {
        IntStream.range(1, 5).forEach((item) -> {
            var asset = getAsset("id" + item);
            sqlAssetIndex.accept(asset, getDataAddress());
        });

        var assetsFound = sqlAssetIndex.queryAssets(getQuerySpec());

        assertThat(assetsFound).isNotNull();
        assertThat(assetsFound.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Find an asset that doesn't exist")
    void findAsset_doesNotExist() {
        assertThat(sqlAssetIndex.findById("id1")).isNull();
    }

    @Test
    @DisplayName("Find an asset that exists")
    void findAsset_exists() {
        var asset = getAsset("id1");
        sqlAssetIndex.accept(asset, getDataAddress());

        var assetFound = sqlAssetIndex.findById("id1");

        assertThat(assetFound).isNotNull();
        assertThat(assetFound).usingRecursiveComparison().isEqualTo(asset);
    }

    @Test
    @DisplayName("Find a data address that doesn't exist")
    void resolveDataAddress_doesNotExist() {
        assertThat(sqlAssetIndex.resolveForAsset("id1")).isNull();
    }

    @Test
    @DisplayName("Find a data address that exists")
    void resolveDataAddress_exists() {
        var asset = getAsset("id1");
        var dataAddress = getDataAddress();
        sqlAssetIndex.accept(asset, dataAddress);

        var dataAddressFound = sqlAssetIndex.resolveForAsset("id1");

        assertThat(dataAddressFound).isNotNull();
        assertThat(dataAddressFound).usingRecursiveComparison().isEqualTo(dataAddress);
    }


    private Asset getAsset(String id) {
        return Asset.Builder.newInstance()
                .id(id)
                .property("key" + id, "value" + id)
                .contentType("type")
                .build();
    }

    private DataAddress getDataAddress() {
        return DataAddress.Builder.newInstance()
                .type("type")
                .property("key", "value")
                .build();
    }

    private QuerySpec getQuerySpec() {
        return QuerySpec.Builder.newInstance()
                .limit(3)
                .offset(2)
                .build();
    }

    private Connection getConnection() throws SQLException {
        return dataSourceRegistry.resolve(DATASOURCE_NAME).getConnection();
    }

}
