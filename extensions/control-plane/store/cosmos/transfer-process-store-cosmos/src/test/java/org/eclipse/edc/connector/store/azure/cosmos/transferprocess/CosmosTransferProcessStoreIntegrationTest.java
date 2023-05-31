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

package org.eclipse.edc.connector.store.azure.cosmos.transferprocess;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.edc.azure.testfixtures.CosmosTestClient;
import org.eclipse.edc.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.edc.connector.store.azure.cosmos.transferprocess.model.TransferProcessDocument;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.testfixtures.store.TransferProcessStoreTestBase;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@AzureCosmosDbIntegrationTest
class CosmosTransferProcessStoreIntegrationTest extends TransferProcessStoreTestBase {

    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String DATABASE_NAME = "transferprocessstore-itest_" + TEST_ID;
    private static final String CONTAINER_PREFIX = "container_";
    private static CosmosContainer container;
    private static CosmosDatabase database;
    private final Clock clock = Clock.systemUTC();
    private final String partitionKey = "testpartition";
    private CosmosTransferProcessStore store;
    private TypeManager typeManager;

    @BeforeAll
    static void prepareCosmosClient() {
        var client = CosmosTestClient.createClient();

        var response = client.createDatabaseIfNotExists(DATABASE_NAME);
        database = client.getDatabase(response.getProperties().getId());
    }

    @AfterAll
    static void cleanup() {
        if (database != null) {
            var databaseDelete = database.delete();
            assertThat(databaseDelete.getStatusCode()).isBetween(200, 400);
        }
    }

    @BeforeEach
    void setUp() {
        assertThat(database).describedAs("CosmosDB database is null - did something go wrong during initialization?").isNotNull();

        typeManager = new TypeManager();
        typeManager.registerTypes(DataRequest.class);

        var containerName = CONTAINER_PREFIX + UUID.randomUUID();
        var containerIfNotExists = database.createContainerIfNotExists(containerName, "/partitionKey");
        container = database.getContainer(containerIfNotExists.getProperties().getId());

        var retryPolicy = RetryPolicy.builder().withMaxRetries(5).withBackoff(1, 3, ChronoUnit.SECONDS).build();
        var cosmosDbApi = new CosmosDbApiImpl(container, false);
        cosmosDbApi.uploadStoredProcedure("nextForState");
        cosmosDbApi.uploadStoredProcedure("lease");
        String connectorId = "test-connector";
        store = new CosmosTransferProcessStore(cosmosDbApi, typeManager, partitionKey, connectorId, retryPolicy, clock);
    }

    @AfterEach
    void tearDown() {
        container.delete();
    }

    @Override
    protected boolean supportsCollectionQuery() {
        return false;
    }

    @Override
    protected boolean supportsLikeOperator() {
        return true;
    }

    @Override
    protected TransferProcessStore getTransferProcessStore() {
        return store;
    }

    @Override
    protected void lockEntity(String negotiationId, String owner, Duration duration) {


        var document = readDocument(negotiationId);
        document.acquireLease(owner, clock, duration);

        var result = container.upsertItem(document, new PartitionKey(partitionKey), new CosmosItemRequestOptions());
        assertThat(result.getStatusCode()).isEqualTo(200);
    }

    @Override
    protected boolean isLockedBy(String negotiationId, String owner) {
        var lease = readDocument(negotiationId).getLease();
        return lease != null && lease.getLeasedBy().equals(owner) && !lease.isExpired(clock.millis());
    }

    private TransferProcessDocument convert(Object obj) {
        return typeManager.readValue(typeManager.writeValueAsBytes(obj), TransferProcessDocument.class);
    }

    private TransferProcessDocument readDocument(String id) {
        CosmosItemResponse<Object> response = container.readItem(id, new PartitionKey(partitionKey), Object.class);
        return convert(response.getItem());
    }
}
