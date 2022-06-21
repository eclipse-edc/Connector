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
 *       Microsoft Corporation - added full QuerySpec support, improvements
 *
 */

package org.eclipse.dataspaceconnector.sql.assetindex;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.common.util.junit.annotations.ComponentTest;
import org.eclipse.dataspaceconnector.dataloading.AssetEntry;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.sql.SqlQueryExecutor;
import org.eclipse.dataspaceconnector.sql.assetindex.schema.BaseSqlDialectStatements;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ComponentTest
public class SqlAssetIndexTest {

    private static final String DATASOURCE_NAME = "asset";

    private SqlAssetIndex sqlAssetIndex;
    private DataSourceRegistry dataSourceRegistry;
    private BaseSqlDialectStatements sqlStatements;
    private TransactionContext transactionContext;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        var txManager = new NoopTransactionContext();
        dataSourceRegistry = mock(DataSourceRegistry.class);

        transactionContext = txManager;
        var jdbcDataSource = new JdbcDataSource();
        jdbcDataSource.setURL("jdbc:h2:mem:");

        // do not actually close
        connection = spy(jdbcDataSource.getConnection());
        doNothing().when(connection).close();

        DataSource datasourceMock = mock(DataSource.class);
        when(datasourceMock.getConnection()).thenReturn(connection);
        when(dataSourceRegistry.resolve(DATASOURCE_NAME)).thenReturn(datasourceMock);

        sqlStatements = new H2DialectStatements();
        sqlAssetIndex = new SqlAssetIndex(dataSourceRegistry, DATASOURCE_NAME, transactionContext, new ObjectMapper(), sqlStatements);


        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        transactionContext.execute(() -> SqlQueryExecutor.executeQuery(connection, schema));
    }

    @AfterEach
    void tearDown() throws Exception {
        doCallRealMethod().when(connection).close();
        connection.close();
    }

    @Test
    @DisplayName("Accept an asset and a data address that don't exist yet")
    void acceptAssetAndDataAddress_doesNotExist() {
        var assetExpected = getAsset("id1");
        sqlAssetIndex.accept(assetExpected, getDataAddress());

        try (var connection = getConnection()) {

            var assetProperties = transactionContext.execute(() -> {
                var assetCount = executeQuery(connection, sqlAssetIndex::mapRowCount, sqlStatements.getCountAssetByIdClause(), assetExpected.getId()).iterator().next();

                if (assetCount <= 0) {
                    return null;
                } else if (assetCount > 1) {
                    throw new IllegalStateException("Expected result set size of 0 or 1 but got " + assetCount);
                }

                return executeQuery(connection, sqlAssetIndex::mapPropertyResultSet, sqlStatements.getFindPropertyByIdTemplate(), assetExpected.getId()).stream().collect(Collectors.toMap(
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
                var assetCount = executeQuery(connection, sqlAssetIndex::mapRowCount, sqlStatements.getCountAssetByIdClause(), assetExpected.getId()).iterator().next();

                if (assetCount <= 0) {
                    return null;
                } else if (assetCount > 1) {
                    throw new IllegalStateException("Expected result set size of 0 or 1 but got " + assetCount);
                }

                return executeQuery(connection, sqlAssetIndex::mapPropertyResultSet, sqlStatements.getFindPropertyByIdTemplate(), assetExpected.getId()).stream().collect(Collectors.toMap(
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
                        String.format("SELECT COUNT(*) as COUNT FROM %s", sqlStatements.getAssetTable())).iterator().next();
                assertThat(assetCount).isEqualTo(0);
                var propCount = executeQuery(connection, sqlAssetIndex::mapRowCount,
                        String.format("SELECT COUNT(*) as COUNT FROM %s", sqlStatements.getAssetPropertyTable())).iterator().next();
                assertThat(assetCount).isEqualTo(0);
                var dataAddressCount = executeQuery(connection, sqlAssetIndex::mapRowCount,
                        String.format("SELECT COUNT(*) as COUNT FROM %s", sqlStatements.getDataAddressTable())).iterator().next();

                assertThat(assetCount).isEqualTo(0);
                assertThat(propCount).isEqualTo(0);
                assertThat(dataAddressCount).isEqualTo(0);
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
    @DisplayName("Query assets with selector expression using the IN operator")
    void queryAsset_selectorExpression_in() {
        var asset1 = getAsset("id1");
        sqlAssetIndex.accept(asset1, getDataAddress());
        var asset2 = getAsset("id2");
        sqlAssetIndex.accept(asset2, getDataAddress());

        var assetsFound = sqlAssetIndex.queryAssets(AssetSelectorExpression.Builder.newInstance()
                .constraint(Asset.PROPERTY_ID, "in", List.of("id1", "id2"))
                .build());

        assertThat(assetsFound).isNotNull();
        assertThat(assetsFound).hasSize(2);
    }

    @Test
    @DisplayName("Query assets with selector expression using the IN operator, invalid righ-operand")
    void queryAsset_selectorExpression_invalidOperand() {
        var asset1 = getAsset("id1");
        sqlAssetIndex.accept(asset1, getDataAddress());
        var asset2 = getAsset("id2");
        sqlAssetIndex.accept(asset2, getDataAddress());

        assertThatThrownBy(() -> sqlAssetIndex.queryAssets(AssetSelectorExpression.Builder.newInstance()
                .constraint(Asset.PROPERTY_ID, "in", "(id1, id2)")
                .build())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Query assets with selector expression using the LIKE operator")
    void queryAsset_selectorExpression_like() {
        var asset1 = getAsset("id1");
        sqlAssetIndex.accept(asset1, getDataAddress());
        var asset2 = getAsset("id2");
        sqlAssetIndex.accept(asset2, getDataAddress());

        var assetsFound = sqlAssetIndex.queryAssets(AssetSelectorExpression.Builder.newInstance()
                .constraint(Asset.PROPERTY_ID, "LIKE", "id%")
                .build());

        assertThat(assetsFound).isNotNull();
        assertThat(assetsFound).hasSize(2);
    }

    @Test
    @DisplayName("Query assets with selector expression using the LIKE operator on a json value")
    void queryAsset_selectorExpression_likeJson() throws JsonProcessingException {
        var asset = getAsset("id1");
        asset.getProperties().put("myjson", new ObjectMapper().writeValueAsString(new TestObject("test123", 42, false)));
        sqlAssetIndex.accept(asset, getDataAddress());

        var assetsFound = sqlAssetIndex.queryAssets(AssetSelectorExpression.Builder.newInstance()
                .constraint("myjson", "LIKE", "%test123%")
                .build());

        assertThat(assetsFound).usingRecursiveFieldByFieldElementComparator().containsExactly(asset);
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
    @DisplayName("Query assets with query spec where the property (=leftOperand) does not exist")
    void queryAsset_querySpec_nonExistProperty() {
        var asset = getAsset("id1");
        sqlAssetIndex.accept(asset, getDataAddress());

        var qs = QuerySpec.Builder
                .newInstance()
                .filter(List.of(new Criterion("noexist", "=", "42")))
                .build();
        assertThat(sqlAssetIndex.queryAssets(qs)).isEmpty();
    }

    @Test
    @DisplayName("Query assets with selector expression using the LIKE operator on a json value")
    void queryAsset_querySpec_likeJson() throws JsonProcessingException {
        var asset = getAsset("id1");
        asset.getProperties().put("myjson", new ObjectMapper().writeValueAsString(new TestObject("test123", 42, false)));
        sqlAssetIndex.accept(asset, getDataAddress());

        var assetsFound = sqlAssetIndex.queryAssets(QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("myjson", "LIKE", "%test123%")))
                .build());

        assertThat(assetsFound).usingRecursiveFieldByFieldElementComparator().containsExactly(asset);
    }

    @Test
    @DisplayName("Query assets with query spec where the value (=rightOperand) does not exist")
    void queryAsset_querySpec_nonExistValue() {
        var asset = getAsset("id1");
        asset.getProperties().put("someprop", "someval");
        sqlAssetIndex.accept(asset, getDataAddress());

        var qs = QuerySpec.Builder
                .newInstance()
                .filter(List.of(new Criterion("someprop", "=", "some-other-val")))
                .build();
        assertThat(sqlAssetIndex.queryAssets(qs)).isEmpty();
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
    void queryAsset_withFilterExpression() {
        var qs = QuerySpec.Builder.newInstance().filter(List.of(
                new Criterion("version", "=", "2.0"),
                new Criterion("content-type", "=", "whatever")
        ));

        var asset = getAsset("id1");
        asset.getProperties().put("version", "2.0");
        asset.getProperties().put("content-type", "whatever");
        sqlAssetIndex.accept(asset, getDataAddress());

        var result = sqlAssetIndex.queryAssets(qs.build()).collect(Collectors.toList());
        assertThat(result).usingRecursiveFieldByFieldElementComparator().containsOnly(asset);

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

    private static class H2DialectStatements extends BaseSqlDialectStatements {
        @Override
        public String getFormatAsJsonOperator() {
            return " FORMAT JSON";
        }
    }
}
