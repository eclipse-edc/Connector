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
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
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
class CosmosFederatedCacheNodeDirectoryTest {

    public static final String REGION = "westeurope";
    private static final String ACCOUNT_NAME = "cosmos-itest";
    private static final String DATABASE_NAME = "connector-itest";
    private static final String CONTAINER_NAME = "CosmosFederatedCatalogNodeStoreTest";
    private static CosmosContainer container;
    private static CosmosDatabase database;
    private final String partitionKey = "testpartition";
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

    @BeforeEach
    void setUp() {
        assertThat(database).describedAs("CosmosDB database is null - did something go wrong during initialization?").isNotNull();
        CosmosContainerResponse containerIfNotExists = database.createContainerIfNotExists(CONTAINER_NAME, "/partitionKey");
        container = database.getContainer(containerIfNotExists.getProperties().getId());
        typeManager = new TypeManager();
        typeManager.registerTypes(FederatedCacheNode.class, FederatedCacheNodeDocument.class);
        store = new CosmosFederatedCacheNodeDirectory(container, partitionKey, typeManager, new RetryPolicy<>());
    }

    @Test
    void create() {
        FederatedCacheNode node = new FederatedCacheNode(UUID.randomUUID().toString(), "http://test.com", Arrays.asList("rest", "ids"));
        store.insert(node);

        CosmosPagedIterable<Object> documents = container.readAllItems(new PartitionKey(partitionKey), Object.class);
        assertThat(documents).hasSize(1)
                .allSatisfy(obj -> {
                    var doc = convert(obj);
                    assertThat(doc.getWrappedInstance()).isEqualTo(node);
                    assertThat(doc.getPartitionKey()).isEqualTo(partitionKey);
                });
    }

    @Test
    void getAll() {
        List<FederatedCacheNode> nodes = Arrays.asList(
                new FederatedCacheNode("test1", "http://test1.com", Collections.singletonList("ids")),
                new FederatedCacheNode("test2", "http://test2.com", Collections.singletonList("rest"))
        );
        container.createItem(
                new FederatedCacheNodeDocument(nodes.get(0), "partition-test"));
        container.createItem(
                new FederatedCacheNodeDocument(nodes.get(1), "partition-test"));

        List<FederatedCacheNode> result = store.getAll();
        assertThat(result).isEqualTo(nodes);
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
