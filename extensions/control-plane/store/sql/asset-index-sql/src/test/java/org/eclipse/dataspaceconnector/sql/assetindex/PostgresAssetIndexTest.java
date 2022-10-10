/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.sql.assetindex;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.common.util.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.dataspaceconnector.policy.model.PolicyRegistrationTypes;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndexTestBase;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.asset.TestObject;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.sql.PostgresqlLocalInstance;
import org.eclipse.dataspaceconnector.sql.assetindex.schema.BaseSqlDialectStatements;
import org.eclipse.dataspaceconnector.sql.assetindex.schema.postgres.PostgresDialectStatements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
import static org.eclipse.dataspaceconnector.sql.assetindex.TestFunctions.createAsset;
import static org.eclipse.dataspaceconnector.sql.assetindex.TestFunctions.createAssetBuilder;
import static org.eclipse.dataspaceconnector.sql.assetindex.TestFunctions.createDataAddress;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@PostgresqlDbIntegrationTest
class PostgresAssetIndexTest extends AssetIndexTestBase {

    private static final String DATASOURCE_NAME = "asset";

    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final BaseSqlDialectStatements sqlStatements = new PostgresDialectStatements();
    private final DataSourceRegistry dataSourceRegistry = mock(DataSourceRegistry.class);
    private final DataSource dataSource = mock(DataSource.class);
    private final Connection connection = spy(PostgresqlLocalInstance.getTestConnection());
    private SqlAssetIndex sqlAssetIndex;

    @BeforeAll
    static void prepare() {
        PostgresqlLocalInstance.createTestDatabase();
    }

    @BeforeEach
    void setUp() throws IOException, SQLException {
        when(dataSourceRegistry.resolve(DATASOURCE_NAME)).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        doNothing().when(connection).close();

        var typeManager = new TypeManager();
        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));

        sqlAssetIndex = new SqlAssetIndex(dataSourceRegistry, DATASOURCE_NAME, transactionContext, new ObjectMapper(), sqlStatements);

        var schema = Files.readString(Paths.get("docs/schema.sql"));
        transactionContext.execute(() -> executeQuery(connection, schema));
    }

    @AfterEach
    void tearDown() throws SQLException {
        transactionContext.execute(() -> {
            executeQuery(connection, "DROP TABLE " + sqlStatements.getAssetTable() + " CASCADE");
            executeQuery(connection, "DROP TABLE " + sqlStatements.getDataAddressTable() + " CASCADE");
            executeQuery(connection, "DROP TABLE " + sqlStatements.getAssetPropertyTable() + " CASCADE");
        });
        doCallRealMethod().when(connection).close();
        connection.close();
    }

    @Test
    void query_byAssetProperty() {
        List<Asset> allAssets = createAssets(5);
        var query = QuerySpec.Builder.newInstance().filter("test-key = test-value1").build();

        assertThat(sqlAssetIndex.queryAssets(query)).usingRecursiveFieldByFieldElementComparator().containsOnly(allAssets.get(1));

    }

    @Test
    void query_byAssetProperty_leftOperandNotExist() {
        createAssets(5);
        var query = QuerySpec.Builder.newInstance().filter("notexist-key = test-value1").build();

        assertThat(sqlAssetIndex.queryAssets(query)).isEmpty();
    }

    @Test
    void verifyCorrectJsonOperator() {
        assertThat(sqlStatements.getFormatAsJsonOperator()).isEqualTo("::json");
    }

    @Test
    void query_assetPropertyAsObject() {
        var asset = createAsset("id1");
        asset.getProperties().put("testobj", new TestObject("test123", 42, false));
        sqlAssetIndex.accept(asset, createDataAddress("test-type"));

        var assetsFound = sqlAssetIndex.queryAssets(AssetSelectorExpression.Builder.newInstance()
                .constraint("testobj", "like", "%test1%")
                .build());

        assertThat(assetsFound).usingRecursiveFieldByFieldElementComparator().containsExactly(asset);
        assertThat(asset.getProperty("testobj")).isInstanceOf(TestObject.class);
    }

    @Test
    void query_byAssetProperty_rightOperandNotExist() {
        createAssets(5);
        var query = QuerySpec.Builder.newInstance().filter("test-key = notexist").build();

        assertThat(sqlAssetIndex.queryAssets(query)).isEmpty();
    }

    @Test
    void queryAgreements_withQuerySpec_invalidOperator() {
        var asset = createAssetBuilder("id1").property("testproperty", "testvalue").build();
        sqlAssetIndex.accept(asset, createDataAddress("test-type"));

        var query = QuerySpec.Builder.newInstance().filter("testproperty <> foobar").build();
        assertThatThrownBy(() -> sqlAssetIndex.queryAssets(query)).isInstanceOf(IllegalArgumentException.class);
    }

    @Override
    protected SqlAssetIndex getAssetIndex() {
        return sqlAssetIndex;
    }

    /**
     * creates a configurable amount of assets with one property ("test-key" = "test-valueN") and a data address of type
     * "test-type"
     */
    private List<Asset> createAssets(int amount) {
        return IntStream.range(0, amount).mapToObj(i -> {
            var asset = createAssetBuilder("test-asset" + i)
                    .property("test-key", "test-value" + i)
                    .build();
            var dataAddress = createDataAddress("test-type");
            sqlAssetIndex.accept(asset, dataAddress);
            return asset;
        }).collect(Collectors.toList());
    }

}