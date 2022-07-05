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

package org.eclipse.dataspaceconnector.catalog.node.directory.azure;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;
import dev.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.dataspaceconnector.azure.testfixtures.CosmosTestClient;
import org.eclipse.dataspaceconnector.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.dataspaceconnector.catalog.node.directory.azure.model.FederatedCacheNodeDocument;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@AzureCosmosDbIntegrationTest
class CosmosFederatedCacheNodeDirectoryIntegrationTest {
    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String TEST_PARTITION_KEY = "test-partitionkey";
    private static final String DATABASE_NAME = "connector-itest-" + TEST_ID;
    private static final String CONTAINER_NAME = "CosmosFederatedCatalogNodeStoreTest-" + TEST_ID;
    private static CosmosContainer container;
    private static CosmosDatabase database;
    private CosmosFederatedCacheNodeDirectory store;
    private TypeManager typeManager;

    @BeforeAll
    static void prepareCosmosClient() {
        var client = CosmosTestClient.createClient();

        var response = client.createDatabaseIfNotExists(DATABASE_NAME);
        database = client.getDatabase(response.getProperties().getId());

    }

    @AfterAll
    static void deleteDatabase() {
        if (database != null) {
            CosmosDatabaseResponse delete = database.delete();
            assertThat(delete.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(300);
        }
    }

    private static boolean assertNodesAreEqual(FederatedCacheNode node1, FederatedCacheNode node2) {
        return node1.getName().equals(node2.getName()) &&
                node1.getTargetUrl().equals(node2.getTargetUrl()) &&
                node1.getSupportedProtocols().equals(node2.getSupportedProtocols());
    }

    @BeforeEach
    void setUp() {
        var containerIfNotExists = database.createContainerIfNotExists(CONTAINER_NAME, "/partitionKey");
        container = database.getContainer(containerIfNotExists.getProperties().getId());
        assertThat(database).describedAs("CosmosDB database is null - did something go wrong during initialization?").isNotNull();
        typeManager = new TypeManager();
        typeManager.registerTypes(FederatedCacheNode.class, FederatedCacheNodeDocument.class);
        var cosmosDbApi = new CosmosDbApiImpl(container, true);
        store = new CosmosFederatedCacheNodeDirectory(cosmosDbApi, TEST_PARTITION_KEY, typeManager, RetryPolicy.ofDefaults());
    }

    @AfterEach
    void teardown() {
        container.delete();
    }

    @Test
    void create() {
        FederatedCacheNode node = new FederatedCacheNode(UUID.randomUUID().toString(), "http://test.com", Arrays.asList("rest", "ids"));
        store.insert(node);

        CosmosPagedIterable<Object> documents = container.readAllItems(new PartitionKey(TEST_PARTITION_KEY), Object.class);
        assertThat(documents).hasSize(1)
                .allSatisfy(obj -> {
                    var doc = convert(obj);
                    assertNodesAreEqual(doc.getWrappedInstance(), node);
                    assertThat(doc.getPartitionKey()).isEqualTo(TEST_PARTITION_KEY);
                });
    }

    @Test
    void getAll() {
        FederatedCacheNode node1 = new FederatedCacheNode("test1", "http://test1.com", Collections.singletonList("ids"));
        FederatedCacheNode node2 = new FederatedCacheNode("test2", "http://test2.com", Collections.singletonList("rest"));
        container.createItem(new FederatedCacheNodeDocument(node1, TEST_PARTITION_KEY));
        container.createItem(new FederatedCacheNodeDocument(node2, TEST_PARTITION_KEY));

        List<FederatedCacheNode> result = store.getAll();

        assertThat(result).hasSize(2)
                .anyMatch(node -> assertNodesAreEqual(node, node1))
                .anyMatch(node -> assertNodesAreEqual(node, node2));
    }

    private FederatedCacheNodeDocument convert(Object obj) {
        return typeManager.readValue(typeManager.writeValueAsBytes(obj), FederatedCacheNodeDocument.class);
    }
}
