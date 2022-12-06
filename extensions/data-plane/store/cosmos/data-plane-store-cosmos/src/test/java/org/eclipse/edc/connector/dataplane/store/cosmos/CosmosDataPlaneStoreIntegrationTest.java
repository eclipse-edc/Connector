/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.store.cosmos;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.edc.azure.testfixtures.CosmosTestClient;
import org.eclipse.edc.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.connector.dataplane.spi.testfixtures.store.DataPlaneStoreTestBase;
import org.eclipse.edc.connector.dataplane.store.cosmos.model.DataFlowRequestDocument;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@AzureCosmosDbIntegrationTest
public class CosmosDataPlaneStoreIntegrationTest extends DataPlaneStoreTestBase {

    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String DATABASE_NAME = "connector-itest-" + TEST_ID;
    private static final String CONTAINER_PREFIX = "DataPlaneStore-";
    private static final String TEST_PARTITION_KEY = "test-part-key";
    private static CosmosContainer container;
    private static CosmosDatabase database;
    private static TypeManager typeManager;

    private DataPlaneStore store;

    @BeforeAll
    static void prepareCosmosClient() {
        var client = CosmosTestClient.createClient();
        typeManager = new TypeManager();
        typeManager.registerTypes(DataFlowRequestDocument.class);

        CosmosDatabaseResponse response = client.createDatabaseIfNotExists(DATABASE_NAME);
        database = client.getDatabase(response.getProperties().getId());
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
        var containerName = CONTAINER_PREFIX + UUID.randomUUID();
        CosmosContainerResponse containerIfNotExists = database.createContainerIfNotExists(containerName, "/partitionKey");
        container = database.getContainer(containerIfNotExists.getProperties().getId());
        assertThat(database).describedAs("CosmosDB database is null - did something go wrong during initialization?").isNotNull();

        var cosmosDbApi = new CosmosDbApiImpl(container, true);
        var retryPolicy = RetryPolicy.builder().withMaxRetries(3).withBackoff(1, 5, ChronoUnit.SECONDS).build();
        store = new CosmosDataPlaneStore(cosmosDbApi, typeManager.getMapper(), retryPolicy, TEST_PARTITION_KEY, Clock.systemUTC());
    }

    @Override
    protected DataPlaneStore getStore() {
        return store;
    }
}
