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
 *       Fraunhofer Institute for Software and Systems Engineering - added tests
 *
 */

package org.eclipse.edc.connector.store.azure.cosmos.contractdefinition;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.edc.azure.testfixtures.CosmosTestClient;
import org.eclipse.edc.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.testfixtures.offer.store.ContractDefinitionStoreTestBase;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.store.azure.cosmos.contractdefinition.model.ContractDefinitionDocument;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.store.azure.cosmos.contractdefinition.TestFunctions.generateDefinition;
import static org.eclipse.edc.connector.store.azure.cosmos.contractdefinition.TestFunctions.generateDocument;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.mockito.Mockito.mock;

@AzureCosmosDbIntegrationTest
class CosmosContractDefinitionStoreIntegrationTest extends ContractDefinitionStoreTestBase {
    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String DATABASE_NAME = "connector-itest-" + TEST_ID;
    private static final String CONTAINER_PREFIX = "ContractDefinitionStore-";
    private static final String TEST_PARTITION_KEY = "test-part-key";
    private static CosmosContainer container;
    private static CosmosDatabase database;
    private static TypeManager typeManager;
    private CosmosContractDefinitionStore store;

    @BeforeAll
    static void prepareCosmosClient() {
        var client = CosmosTestClient.createClient();
        typeManager = new TypeManager();
        typeManager.registerTypes(ContractDefinition.class, ContractDefinitionDocument.class);

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
        store = new CosmosContractDefinitionStore(cosmosDbApi, typeManager, retryPolicy, TEST_PARTITION_KEY, mock(Monitor.class));
    }

    @AfterEach
    void tearDown() {
        container.delete();
    }

    // Override the base test since Cosmos returns documents where the property subject of ORDER BY does not exist
    @Test
    void findAll_verifySorting_invalidProperty() {
        var ids = IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PARTITION_KEY)).peek(d -> container.createItem(d)).map(ContractDefinitionDocument::getId).collect(Collectors.toList());


        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

        var all = store.findAll(query).collect(Collectors.toList());
        assertThat(all).hasSize(10);
    }

    @Test
    void verify_readWriteFindAll() {
        // add an object
        var def = generateDefinition();
        store.save(def);
        assertThat(store.findAll(QuerySpec.max())).containsExactly(def);

        // modify the object
        var modifiedDef = ContractDefinition.Builder.newInstance().id(def.getId())
                .contractPolicyId("test-cp-id-new")
                .accessPolicyId("test-ap-id-new")
                .assetsSelectorCriterion(criterion("somekey", "=", "someval"))
                .build();

        store.update(modifiedDef);

        var querySpec = QuerySpec.Builder.newInstance().filter(criterion("contractPolicyId", "=", "test-cp-id-new")).build();

        var all = store.findAll(querySpec).collect(Collectors.toList());

        assertThat(all).hasSize(1).containsExactly(modifiedDef);
    }

    @Override
    protected ContractDefinitionStore getContractDefinitionStore() {
        return store;
    }

    @Override
    protected boolean supportsCollectionQuery() {
        return true;
    }

    @Override
    protected boolean supportsCollectionIndexQuery() {
        return true;
    }

}
