/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.catalog.node.directory.azure.model.FederatedCacheNodeDocument;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNode;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApiImpl;
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
import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;

@IntegrationTest
class CosmosFederatedCacheNodeDirectoryIntegrationTest {
    public static final String REGION = "westeurope";
    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String ACCOUNT_NAME = "cosmos-itest";
    private static final String TEST_PARTITION_KEY = "test-partitionkey";
    private static final String DATABASE_NAME = "connector-itest-" + TEST_ID;
    private static final String CONTAINER_NAME = "CosmosFederatedCatalogNodeStoreTest-" + TEST_ID;
    private static CosmosContainer container;
    private static CosmosDatabase database;
    private CosmosFederatedCacheNodeDirectory store;
    private TypeManager typeManager;

    @BeforeAll
    static void prepareCosmosClient() {
        var key = propOrEnv("COSMOS_KEY", null);
        if (key != null) {
            var client = new CosmosClientBuilder()
                    .key(key)
                    .preferredRegions(Collections.singletonList(REGION))
                    .consistencyLevel(ConsistencyLevel.SESSION)
                    .endpoint("https://" + ACCOUNT_NAME + ".documents.azure.com:443/")
                    .buildClient();

            CosmosDatabaseResponse response = client.createDatabaseIfNotExists(DATABASE_NAME);
            database = client.getDatabase(response.getProperties().getId());
        }
    }

    private static boolean assertNodesAreEqual(FederatedCacheNode node1, FederatedCacheNode node2) {
        return node1.getName().equals(node2.getName()) &&
                node1.getTargetUrl().equals(node2.getTargetUrl()) &&
                node1.getSupportedProtocols().equals(node2.getSupportedProtocols());
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
        CosmosContainerResponse containerIfNotExists = database.createContainerIfNotExists(CONTAINER_NAME, "/partitionKey");
        container = database.getContainer(containerIfNotExists.getProperties().getId());
        typeManager = new TypeManager();
        typeManager.registerTypes(FederatedCacheNode.class, FederatedCacheNodeDocument.class);
        CosmosDbApi cosmosDbApi = new CosmosDbApiImpl(container, true);
        store = new CosmosFederatedCacheNodeDirectory(cosmosDbApi, TEST_PARTITION_KEY, typeManager, new RetryPolicy<>());
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

    @AfterEach
    void teardown() {
        CosmosContainerResponse delete = container.delete();
        assertThat(delete.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(300);
    }

    private FederatedCacheNodeDocument convert(Object obj) {
        return typeManager.readValue(typeManager.writeValueAsBytes(obj), FederatedCacheNodeDocument.class);
    }
}
