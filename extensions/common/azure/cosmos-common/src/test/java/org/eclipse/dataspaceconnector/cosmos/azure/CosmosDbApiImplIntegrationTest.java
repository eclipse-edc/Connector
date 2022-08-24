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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.cosmos.azure;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.dataspaceconnector.azure.testfixtures.CosmosTestClient;
import org.eclipse.dataspaceconnector.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@AzureCosmosDbIntegrationTest
class CosmosDbApiImplIntegrationTest {

    public static final String PARTITION_KEY = "partitionKey";
    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String DATABASE_NAME = "connector-itest-" + TEST_ID;
    private static final String CONTAINER_NAME = "CosmosAssetIndexTest-" + TEST_ID;
    private static CosmosContainer container;
    private static CosmosDatabase database;
    private CosmosDbApiImpl cosmosDbApi;
    private ArrayList<TestCosmosDocument> record;

    @BeforeAll
    static void prepare() {
        var client = CosmosTestClient.createClient();

        CosmosDatabaseResponse response = client.createDatabaseIfNotExists(DATABASE_NAME);
        database = client.getDatabase(response.getProperties().getId());
    }

    @AfterAll
    static void cleanup() {
        if (database != null) {
            CosmosDatabaseResponse delete = database.delete();
            assertThat(delete.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(300);
        }
    }

    @BeforeEach
    void setup() {
        assertThat(database).describedAs("CosmosDB database is null - did something go wrong during initialization?").isNotNull();
        CosmosContainerResponse containerIfNotExists = database.createContainerIfNotExists(CONTAINER_NAME, "/partitionKey");
        container = database.getContainer(containerIfNotExists.getProperties().getId());
        cosmosDbApi = new CosmosDbApiImpl(container, true);

        record = new ArrayList<>();
    }

    @AfterEach
    void teardown() {
        record.forEach(td -> container.deleteItem(td, new CosmosItemRequestOptions()));
    }

    @Test
    void createItem() {
        var testItem = new TestCosmosDocument("payload", PARTITION_KEY);
        cosmosDbApi.saveItem(testItem);
        record.add(testItem);

        assertThat(container.readAllItems(new PartitionKey(PARTITION_KEY), Object.class)).hasSize(1);
    }

    @Test
    void queryItemById() {
        var testItem = new TestCosmosDocument("payload", PARTITION_KEY);
        container.createItem(testItem);
        record.add(testItem);

        var queryResult = cosmosDbApi.queryItemById(testItem.getId());

        assertThat(queryResult)
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("id", testItem.getId())
                .containsEntry("partitionKey", PARTITION_KEY)
                .containsEntry("wrappedInstance", "payload");

    }

    @Test
    void queryAllItems() {
        var testItem = new TestCosmosDocument("payload", PARTITION_KEY);
        container.createItem(testItem);
        record.add(testItem);

        var testItem2 = new TestCosmosDocument("payload", PARTITION_KEY);
        container.createItem(testItem2);
        record.add(testItem2);

        assertThat(cosmosDbApi.queryAllItems()).hasSize(2)
                .allSatisfy(o -> {
                    assertThat(o)
                            .asInstanceOf(InstanceOfAssertFactories.MAP)
                            .containsEntry("wrappedInstance", "payload")
                            .hasEntrySatisfying("id", id -> assertThat(id).isIn(testItem.getId(), testItem2.getId()));
                });
    }

    @Test
    void queryItems() {
        // not picked up, wrong payload
        var testItem = new TestCosmosDocument("payload", PARTITION_KEY);
        container.createItem(testItem);
        record.add(testItem);

        var testItem2 = new TestCosmosDocument("payload-two", PARTITION_KEY);
        container.createItem(testItem2);
        record.add(testItem2);

        //should not be picked up - wrong case
        var testItem3 = new TestCosmosDocument("Payload-two", PARTITION_KEY);
        container.createItem(testItem3);
        record.add(testItem3);

        // should be picked up, despite the different partition key
        var testItem4 = new TestCosmosDocument("payload-two", "another-partkey");
        container.createItem(testItem4);
        record.add(testItem4);

        var query = "SELECT * FROM t WHERE t.wrappedInstance='payload-two'";
        var result = cosmosDbApi.queryItems(query);
        assertThat(result).hasSize(2);
    }

    @Test
    void deleteItem_whenItemPresent_deletes() {
        var testItem = new TestCosmosDocument("payload", PARTITION_KEY);
        container.createItem(testItem);

        var deletedItem = cosmosDbApi.deleteItem(testItem.getId());

        assertThat(deletedItem)
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("id", testItem.getId())
                .containsEntry("partitionKey", PARTITION_KEY)
                .containsEntry("wrappedInstance", "payload");
    }

    @Test
    void deleteItem_whenItemMissing_throws() {
        var id = "not-exists";
        assertThatThrownBy(() -> cosmosDbApi.deleteItem(id))
                .hasMessageContaining(String.format("An object with the ID %s could not be found!", id))
                .isInstanceOf(NotFoundException.class);
    }

}