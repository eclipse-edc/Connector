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

package org.eclipse.edc.connector.store.azure.cosmos.assetindex;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.azure.cosmos.models.PartitionKey;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.edc.azure.testfixtures.CosmosTestClient;
import org.eclipse.edc.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.edc.connector.store.azure.cosmos.assetindex.model.AssetDocument;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.testfixtures.asset.AssetIndexTestBase;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;
import static org.mockito.Mockito.mock;

@AzureCosmosDbIntegrationTest
class CosmosAssetIndexIntegrationTest extends AssetIndexTestBase {
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
        assetIndex = new CosmosAssetIndex(api, TEST_PARTITION_KEY, typeManager, RetryPolicy.ofDefaults(), mock(Monitor.class));
    }

    @AfterEach
    void tearDown() {
        // Delete items one by one as deleteAllItemsByPartitionKey is disabled by default on new Cosmos DB accounts.
        PartitionKey partitionKey = new PartitionKey(TEST_PARTITION_KEY);
        container.readAllItems(partitionKey, CosmosDbEntity.class)
                .stream().parallel()
                .forEach(i -> container.deleteItem(i.id, partitionKey, null));
    }

    @Test
    void findById() {
        Asset asset1 = createAssetWithProperty("123", "test", "world");

        Asset asset2 = createAssetWithProperty("456", "test", "bar");

        container.createItem(new AssetDocument(asset1, TEST_PARTITION_KEY, dataAddress));
        container.createItem(new AssetDocument(asset2, TEST_PARTITION_KEY, dataAddress));

        Asset asset = assetIndex.findById("456");

        assertThat(asset).isNotNull();
        assertThat(asset.getProperties()).isEqualTo(asset2.getProperties());
    }

    @Test
    void findAll_withPaging() {
        IntStream.range(0, 10).mapToObj(i -> createAssetWithProperty("id" + i, "foo", "bar" + i))
                .forEach(a -> container.createItem(new AssetDocument(a, TEST_PARTITION_KEY, dataAddress)));

        var limitQuery = QuerySpec.Builder.newInstance().limit(5).offset(2).build();

        var all = assetIndex.queryAssets(limitQuery);
        assertThat(all).hasSize(5).extracting(Asset::getId).containsExactly("id2", "id3", "id4", "id5", "id6");
    }

    @Test
    void findAll_withPaging_sortedDesc() {
        IntStream.range(0, 10).mapToObj(i -> createAssetWithProperty("id" + i, "foo", "bar" + i))
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
        IntStream.range(0, 10).mapToObj(i -> createAssetWithProperty("id" + i, "foo", "bar" + i))
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
    void findAll_withSorting() {
        IntStream.range(5, 10).mapToObj(i -> createAssetWithProperty("id" + i, "foo", "bar" + i))
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
        Asset asset = createAssetWithProperty(UUID.randomUUID().toString(), "test", "foobar");
        container.createItem(new AssetDocument(asset, TEST_PARTITION_KEY, dataAddress));

        var deleteResult = assetIndex.deleteById(asset.getId());
        assertThat(deleteResult.succeeded()).isTrue();
        assertThat(deleteResult.getContent().getProperties()).isEqualTo(asset.getProperties());
        assertThat(assetIndex.findById(asset.getId())).isNull();
    }

    @Test
    void deleteById_whenMissing_returnsNull() {
        assertThat(assetIndex.deleteById("not-exists"))
                .isNotNull()
                .extracting(StoreResult::reason)
                .isEqualTo(NOT_FOUND);
    }

    @Override
    protected AssetIndex getAssetIndex() {
        return assetIndex;
    }

    private Asset createAssetWithProperty(String id, String somePropertyKey, String somePropertyValue) {
        return Asset.Builder.newInstance()
                .id(id)
                .property(somePropertyKey, somePropertyValue)
                .build();
    }

    static class CosmosDbEntity {
        String id;

        public void setId(String id) {
            this.id = id;
        }
    }
}
