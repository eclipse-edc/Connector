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

package org.eclipse.dataspaceconnector.assetindex.azure;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.assetindex.azure.model.AssetDocument;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.dataspaceconnector.azure.testfixtures.CosmosTestClient;
import org.eclipse.dataspaceconnector.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@AzureCosmosDbIntegrationTest
class CosmosAssetIndexIntegrationTest {
    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String DATABASE_NAME = "connector-itest-" + TEST_ID;
    private static final String CONTAINER_NAME = "CosmosAssetIndexTest-" + TEST_ID;
    private static final String TEST_PARTITION_KEY = "test-partitionkey";
    private static CosmosContainer container;
    private static CosmosDatabase database;
    private final DataAddress dataAddress = DataAddress.Builder.newInstance().type("testtype").build();
    private CosmosAssetIndex assetIndex;

    @BeforeAll
    static void prepareCosmosClient() {
        var client = CosmosTestClient.createClient();

        CosmosDatabaseResponse response = client.createDatabaseIfNotExists(DATABASE_NAME);
        database = client.getDatabase(response.getProperties().getId());
        var containerIfNotExists = database.createContainerIfNotExists(CONTAINER_NAME, "/partitionKey");
        container = database.getContainer(containerIfNotExists.getProperties().getId());
    }

    @AfterAll
    static void deleteDatabase() {
        if (database != null) {
            CosmosDatabaseResponse delete = database.delete();
            assertThat(delete.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(300);
        }
    }

    @BeforeEach
    void setUp() {
        assertThat(database).describedAs("CosmosDB database is null - did something go wrong during initialization?").isNotNull();

        TypeManager typeManager = new TypeManager();
        typeManager.registerTypes(Asset.class, AssetDocument.class);
        var api = new CosmosDbApiImpl(container, true);
        assetIndex = new CosmosAssetIndex(api, TEST_PARTITION_KEY, typeManager, new RetryPolicy<>(), mock(Monitor.class));
    }

    @AfterEach
    void tearDown() {
        container.deleteAllItemsByPartitionKey(new PartitionKey(TEST_PARTITION_KEY), new CosmosItemRequestOptions());
    }

    @Test
    void queryAssets_selectAll() {
        Asset asset1 = createAsset("123", "hello", "world");

        Asset asset2 = createAsset("456", "foo", "bar");

        container.createItem(new AssetDocument(asset1, TEST_PARTITION_KEY, dataAddress));
        container.createItem(new AssetDocument(asset2, TEST_PARTITION_KEY, dataAddress));

        List<Asset> assets = assetIndex.queryAssets(AssetSelectorExpression.SELECT_ALL).collect(Collectors.toList());

        assertThat(assets).hasSize(2)
                .anyMatch(asset -> asset.getProperties().equals(asset1.getProperties()))
                .anyMatch(asset -> asset.getProperties().equals(asset2.getProperties()));
    }

    @Test
    void queryAssets_filterOnProperty() {
        Asset asset1 = createAsset("123", "test", "world");

        Asset asset2 = createAsset("456", "test", "bar");

        container.createItem(new AssetDocument(asset1, TEST_PARTITION_KEY, dataAddress));
        container.createItem(new AssetDocument(asset2, TEST_PARTITION_KEY, dataAddress));

        AssetSelectorExpression expression = AssetSelectorExpression.Builder.newInstance()
                .whenEquals(Asset.PROPERTY_ID, "456")
                .build();

        List<Asset> assets = assetIndex.queryAssets(expression).collect(Collectors.toList());

        assertThat(assets).hasSize(1)
                .allSatisfy(asset -> assertThat(asset.getId()).isEqualTo("456"));
    }

    @Test
    void queryAssets_filterOnPropertyContainingIllegalArgs() {
        Asset asset1 = createAsset("123", "test:value", "world");

        Asset asset2 = createAsset("456", "test:value", "bar");

        container.createItem(new AssetDocument(asset1, TEST_PARTITION_KEY, dataAddress));
        container.createItem(new AssetDocument(asset2, TEST_PARTITION_KEY, dataAddress));

        AssetSelectorExpression expression = AssetSelectorExpression.Builder.newInstance()
                .whenEquals("test:value", "bar")
                .build();

        List<Asset> assets = assetIndex.queryAssets(expression).collect(Collectors.toList());

        assertThat(assets).hasSize(1)
                .allSatisfy(asset -> assertThat(asset.getId()).isEqualTo("456"));
    }

    @Test
    void findById() {
        Asset asset1 = createAsset("123", "test", "world");

        Asset asset2 = createAsset("456", "test", "bar");

        container.createItem(new AssetDocument(asset1, TEST_PARTITION_KEY, dataAddress));
        container.createItem(new AssetDocument(asset2, TEST_PARTITION_KEY, dataAddress));

        Asset asset = assetIndex.findById("456");

        assertThat(asset).isNotNull();
        assertThat(asset.getProperties()).isEqualTo(asset2.getProperties());
    }

    @Test
    void queryAssets_operatorIn() {
        Asset asset1 = createAsset("123", "hello", "world");

        Asset asset2 = createAsset("456", "foo", "bar");

        container.createItem(new AssetDocument(asset1, TEST_PARTITION_KEY, dataAddress));
        container.createItem(new AssetDocument(asset2, TEST_PARTITION_KEY, dataAddress));

        var inExpr = format("('%s', '%s')", asset1.getId(), asset2.getId());
        var selector = AssetSelectorExpression.Builder.newInstance()
                .constraint(Asset.PROPERTY_ID, "IN", inExpr)
                .build();

        List<Asset> assets = assetIndex.queryAssets(selector).collect(Collectors.toList());

        assertThat(assets).hasSize(2)
                .anyMatch(asset -> asset.getProperties().equals(asset1.getProperties()))
                .anyMatch(asset -> asset.getProperties().equals(asset2.getProperties()));
    }

    @Test
    void queryAssets_operatorIn_noUpTicks() {
        Asset asset1 = createAsset("123", "hello", "world");

        Asset asset2 = createAsset("456", "foo", "bar");

        container.createItem(new AssetDocument(asset1, TEST_PARTITION_KEY, dataAddress));
        container.createItem(new AssetDocument(asset2, TEST_PARTITION_KEY, dataAddress));

        var inExpr = format("(%s, %s)", asset1.getId(), asset2.getId());
        var selector = AssetSelectorExpression.Builder.newInstance()
                .constraint(Asset.PROPERTY_ID, "IN", inExpr)
                .build();

        List<Asset> assets = assetIndex.queryAssets(selector).collect(Collectors.toList());

        assertThat(assets).isEmpty();
    }

    @Test
    void queryAssets_operatorIn_noBrackets_throwsException() {
        Asset asset1 = createAsset("123", "hello", "world");

        Asset asset2 = createAsset("456", "foo", "bar");

        container.createItem(new AssetDocument(asset1, TEST_PARTITION_KEY, dataAddress));
        container.createItem(new AssetDocument(asset2, TEST_PARTITION_KEY, dataAddress));

        var inExpr = format("'%s', '%s'", asset1.getId(), asset2.getId());
        var selector = AssetSelectorExpression.Builder.newInstance()
                .constraint(Asset.PROPERTY_ID, "IN", inExpr)
                .build();

        // collecting is necessary, otherwise the cosmos query is not executed
        assertThatThrownBy(() -> assetIndex.queryAssets(selector).collect(Collectors.toList())).isInstanceOf(CosmosException.class);

    }

    @Test
    void queryAssets_operatorIn_syntaxError_throwsException() {
        Asset asset1 = createAsset("123", "hello", "world");

        Asset asset2 = createAsset("456", "foo", "bar");

        container.createItem(new AssetDocument(asset1, TEST_PARTITION_KEY, dataAddress));
        container.createItem(new AssetDocument(asset2, TEST_PARTITION_KEY, dataAddress));

        var inExpr = format("('%s' ; '%s')", asset1.getId(), asset2.getId());
        var selector = AssetSelectorExpression.Builder.newInstance()
                .constraint(Asset.PROPERTY_ID, "IN", inExpr)
                .build();

        // collecting is necessary, otherwise the cosmos query is not executed
        assertThatThrownBy(() -> assetIndex.queryAssets(selector).collect(Collectors.toList())).isInstanceOf(CosmosException.class);
    }

    @Test
    void queryAssets_operatorIn_notFound() {

        var inExpr = "('not-exist1', 'not-exist2')";
        var selector = AssetSelectorExpression.Builder.newInstance()
                .constraint(Asset.PROPERTY_ID, "IN", inExpr)
                .build();

        List<Asset> assets = assetIndex.queryAssets(selector).collect(Collectors.toList());

        assertThat(assets).isEmpty();
    }

    @Test
    void findAll_noQuerySpec() {
        Asset asset1 = createAsset("123", "test", "world");
        container.createItem(new AssetDocument(asset1, TEST_PARTITION_KEY, dataAddress));

        var all = assetIndex.queryAssets(QuerySpec.none());
        assertThat(all).hasSize(1).extracting(Asset::getId).containsExactly(asset1.getId());

    }

    @Test
    void findAll_withPaging() {
        IntStream.range(0, 10).mapToObj(i -> createAsset("id" + i, "foo", "bar" + i))
                .forEach(a -> container.createItem(new AssetDocument(a, TEST_PARTITION_KEY, dataAddress)));

        var limitQuery = QuerySpec.Builder.newInstance().limit(5).offset(2).build();

        var all = assetIndex.queryAssets(limitQuery);
        assertThat(all).hasSize(5).extracting(Asset::getId).containsExactly("id2", "id3", "id4", "id5", "id6");
    }

    @Test
    void findAll_withPaging_sortedDesc() {
        IntStream.range(0, 10).mapToObj(i -> createAsset("id" + i, "foo", "bar" + i))
                .forEach(a -> container.createItem(new AssetDocument(a, TEST_PARTITION_KEY, dataAddress)));

        var limitQuery = QuerySpec.Builder.newInstance()
                .limit(5).offset(2)
                .sortField(AssetDocument.sanitize(Asset.PROPERTY_ID))
                .sortOrder(SortOrder.DESC)
                .build();

        var all = assetIndex.queryAssets(limitQuery);
        assertThat(all).hasSize(5).extracting(Asset::getId).containsExactly("id7", "id6", "id5", "id4", "id3");
    }

    @Test
    void findAll_withPaging_sortedAsc() {
        IntStream.range(0, 10).mapToObj(i -> createAsset("id" + i, "foo", "bar" + i))
                .forEach(a -> container.createItem(new AssetDocument(a, TEST_PARTITION_KEY, dataAddress)));

        var limitQuery = QuerySpec.Builder.newInstance()
                .limit(3).offset(2)
                .sortField(AssetDocument.sanitize(Asset.PROPERTY_ID))
                .sortOrder(SortOrder.ASC)
                .build();

        var all = assetIndex.queryAssets(limitQuery);
        assertThat(all).hasSize(3).extracting(Asset::getId).containsExactly("id2", "id3", "id4");
    }

    @Test
    void findAll_withFiltering() {
        IntStream.range(0, 5).mapToObj(i -> createAsset("id" + i, "foo", "bar" + i))
                .forEach(a -> container.createItem(new AssetDocument(a, TEST_PARTITION_KEY, dataAddress)));

        var filterQuery = QuerySpec.Builder.newInstance()
                .equalsAsContains(false)
                .filter("foo=bar4")
                .build();

        var all = assetIndex.queryAssets(filterQuery);
        assertThat(all).hasSize(1).extracting(a -> a.getProperty("foo")).containsOnly("bar4");
    }

    @Test
    void findAll_withInvalidFilter_throwsException() {
        IntStream.range(0, 5).mapToObj(i -> createAsset("id" + i, "foo", "bar" + i))
                .forEach(a -> container.createItem(new AssetDocument(a, TEST_PARTITION_KEY, dataAddress)));

        var filterQuery = QuerySpec.Builder.newInstance()
                .equalsAsContains(false)
                .filter("foo STARTSWITH bar4")
                .build();

        assertThatThrownBy(() -> assetIndex.queryAssets(filterQuery)).isInstanceOf(IllegalArgumentException.class).hasMessage("Cannot build SqlParameter for operator: STARTSWITH");
    }

    @Test
    void findAll_withFilteringOperatorIn_limitExceedsResultSize() {
        IntStream.range(0, 5).mapToObj(i -> createAsset("id" + i, "foo", "bar" + i))
                .forEach(a -> container.createItem(new AssetDocument(a, TEST_PARTITION_KEY, dataAddress)));

        var filterQuery = QuerySpec.Builder.newInstance()
                .equalsAsContains(false)
                .filter("foo IN ('bar4', 'bar3', 'bar2', 'bar1')")
                .limit(10)
                .build();

        var all = assetIndex.queryAssets(filterQuery);
        assertThat(all).hasSize(4).extracting(Asset::getId).containsExactlyInAnyOrder("id4", "id3", "id2", "id1");
    }

    @Test
    void findAll_withSorting() {
        IntStream.range(5, 10).mapToObj(i -> createAsset("id" + i, "foo", "bar" + i))
                .forEach(a -> container.createItem(new AssetDocument(a, TEST_PARTITION_KEY, dataAddress)));

        var sortQuery = QuerySpec.Builder.newInstance()
                .sortOrder(SortOrder.DESC)
                .sortField("foo")
                .build();

        var all = assetIndex.queryAssets(sortQuery);
        assertThat(all).hasSize(5).extracting(Asset::getId).containsExactly("id9", "id8", "id7", "id6", "id5");

    }

    @Test
    void deleteById_whenPresent_deletes() {
        Asset asset = createAsset(UUID.randomUUID().toString(), "test", "foobar");
        container.createItem(new AssetDocument(asset, TEST_PARTITION_KEY, dataAddress));

        Asset deletedAsset = assetIndex.deleteById(asset.getId());
        assertThat(deletedAsset.getProperties()).isEqualTo(asset.getProperties());
        assertThat(assetIndex.findById(asset.getId())).isNull();
    }

    @Test
    void deleteById_whenMissing_returnsNull() {
        assertThat(assetIndex.deleteById("not-exists")).isNull();
    }

    private Asset createAsset(String id, String somePropertyKey, String somePropertyValue) {
        return Asset.Builder.newInstance()
                .id(id)
                .property(somePropertyKey, somePropertyValue)
                .build();
    }
}
