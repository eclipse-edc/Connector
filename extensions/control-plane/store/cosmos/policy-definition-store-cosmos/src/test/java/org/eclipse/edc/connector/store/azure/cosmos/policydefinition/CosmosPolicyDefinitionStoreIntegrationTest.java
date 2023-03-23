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

package org.eclipse.edc.connector.store.azure.cosmos.policydefinition;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.azure.cosmos.models.PartitionKey;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.edc.azure.testfixtures.CosmosTestClient;
import org.eclipse.edc.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.policy.spi.testfixtures.store.PolicyDefinitionStoreTestBase;
import org.eclipse.edc.connector.store.azure.cosmos.policydefinition.model.PolicyDocument;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
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
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.store.azure.cosmos.policydefinition.TestFunctions.generateDocument;
import static org.eclipse.edc.connector.store.azure.cosmos.policydefinition.TestFunctions.generatePolicy;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;
import static org.mockito.Mockito.mock;

@AzureCosmosDbIntegrationTest
public class CosmosPolicyDefinitionStoreIntegrationTest extends PolicyDefinitionStoreTestBase {
    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String DATABASE_NAME = "connector-itest-" + TEST_ID;
    private static final String CONTAINER_PREFIX = "ContractPolicyDefinitionStore-";
    private static final String TEST_PARTITION_KEY = "test-part-key";
    private static CosmosContainer container;
    private static CosmosDatabase database;
    private static TypeManager typeManager;
    private CosmosPolicyDefinitionStore store;

    @BeforeAll
    static void prepareCosmosClient() {
        var client = CosmosTestClient.createClient();
        typeManager = new TypeManager();
        typeManager.registerTypes(PolicyDefinition.class, PolicyDocument.class);
        PolicyRegistrationTypes.TYPES.forEach(typeManager::registerTypes);

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
        store = new CosmosPolicyDefinitionStore(cosmosDbApi, typeManager, retryPolicy, TEST_PARTITION_KEY, mock(Monitor.class));
    }

    @AfterEach
    void tearDown() {
        container.delete();
    }

    @Test
    void findAll() {
        var doc1 = generateDocument(TEST_PARTITION_KEY);
        var doc2 = generateDocument(TEST_PARTITION_KEY);
        container.createItem(doc1);
        container.createItem(doc2);

        assertThat(store.findAll(QuerySpec.none())).hasSize(2).containsExactlyInAnyOrder(doc1.getWrappedInstance(), doc2.getWrappedInstance());
    }

    @Test
    void findAll_noReload() {
        var doc1 = generateDocument(TEST_PARTITION_KEY);
        var doc2 = generateDocument(TEST_PARTITION_KEY);
        container.createItem(doc1);
        container.createItem(doc2);

        assertThat(store.findAll(QuerySpec.none())).hasSize(2);
    }

    @Test
    void findById() {
        var doc1 = generateDocument(TEST_PARTITION_KEY);
        container.createItem(doc1);

        store.reload();

        assertThat(store.findById(doc1.getId())).isEqualTo(doc1.getWrappedInstance());
    }

    @Test
    void findById_notExist() {
        assertThat(store.findById("not-exist")).isNull();
    }

    @Test
    void findAll_emptyResult() {
        assertThat(store.findAll(QuerySpec.none())).isNotNull().isEmpty();
    }

    @Test
    void save() {
        var policy = generatePolicy();
        store.create(policy);

        var actual = container.readAllItems(new PartitionKey(TEST_PARTITION_KEY), Object.class);
        assertThat(actual).hasSize(1);
        var doc = actual.stream().findFirst().get();
        assertThat(convert(doc)).isEqualTo(policy);
    }

    @Test
    void save_exists_shouldFail() {
        var doc1 = generateDocument(TEST_PARTITION_KEY);
        container.createItem(doc1);

        var policyToUpdate = doc1.getWrappedInstance();

        //modify a single field
        policyToUpdate.getPolicy().getPermissions().add(Permission.Builder.newInstance().target("test-permission-target").build());
        policyToUpdate.getPolicy().getObligations().add(Duty.Builder.newInstance().uid("test-obligation-id").build());


        var saveResult = store.create(policyToUpdate);
        assertThat(saveResult.succeeded()).isFalse();
        assertThat(saveResult.reason()).isEqualTo(ALREADY_EXISTS);

        var actual = container.readAllItems(new PartitionKey(doc1.getPartitionKey()), Object.class);
        assertThat(actual).hasSize(1);

        var first = convert(actual.stream().findFirst().get());

        assertThat(first.getPolicy().getPermissions()).isEmpty();
        assertThat(first.getPolicy().getObligations()).isEmpty();
    }

    @Test
    void delete() {
        var document = generateDocument(TEST_PARTITION_KEY);
        container.createItem(document);

        var policy = convert(document);
        var deletedPolicy = store.delete(document.getId());
        assertThat(deletedPolicy.succeeded()).isTrue();
        assertThat(deletedPolicy.getContent()).isEqualTo(policy);

        assertThat(container.readAllItems(new PartitionKey(document.getPartitionKey()), Object.class)).isEmpty();
    }

    @Test
    void findAll_noQuerySpec() {
        var doc1 = generateDocument(TEST_PARTITION_KEY);
        var doc2 = generateDocument(TEST_PARTITION_KEY);

        container.createItem(doc1);
        container.createItem(doc2);

        assertThat(store.findAll(QuerySpec.none())).hasSize(2).extracting(PolicyDefinition::getUid).containsExactlyInAnyOrder(doc1.getId(), doc2.getId());
    }

    @Test
    void findAll_verifyPaging() {

        var all = IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PARTITION_KEY)).peek(d -> container.createItem(d)).map(PolicyDocument::getId).collect(Collectors.toList());

        // page size fits
        assertThat(store.findAll(QuerySpec.Builder.newInstance().offset(3).limit(4).build())).hasSize(4).extracting(PolicyDefinition::getUid).isSubsetOf(all);

    }

    @Test
    void findAll_verifyPaging_pageSizeLargerThanCollection() {

        var all = IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PARTITION_KEY)).peek(d -> container.createItem(d)).map(PolicyDocument::getId).collect(Collectors.toList());

        // page size fits
        assertThat(store.findAll(QuerySpec.Builder.newInstance().offset(3).limit(40).build())).hasSize(7).extracting(PolicyDefinition::getUid).isSubsetOf(all);
    }

    @Test
    void findAll_verifyFiltering() {
        var documents = IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PARTITION_KEY)).peek(d -> container.createItem(d)).collect(Collectors.toList());

        var expectedId = documents.get(3).getId();

        var query = QuerySpec.Builder.newInstance().filter("id=" + expectedId).build();
        assertThat(store.findAll(query)).extracting(PolicyDefinition::getUid).containsOnly(expectedId);
    }

    @Test
    void findAll_verifyFiltering_unsuccessfulFilterExpression() {
        IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PARTITION_KEY)).forEach(d -> container.createItem(d));

        var query = QuerySpec.Builder.newInstance().filter("something = other").build();

        assertThat(store.findAll(query)).isEmpty();
    }

    @Test
    void findAll_verifySorting() {

        IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PARTITION_KEY)).forEach(d -> container.createItem(d));

        var ascendingQuery = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build();
        assertThat(store.findAll(ascendingQuery)).hasSize(10).isSortedAccordingTo(Comparator.comparing(PolicyDefinition::getUid));
        var descendingQuery = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build();
        assertThat(store.findAll(descendingQuery)).hasSize(10).isSortedAccordingTo((c1, c2) -> c2.getUid().compareTo(c1.getUid()));
    }

    // Override the base test since Cosmos returns documents where the property subject of ORDER BY does not exist
    @Test
    void findAll_sorting_nonExistentProperty() {

        var ids = IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PARTITION_KEY)).peek(d -> container.createItem(d)).map(PolicyDocument::getId).collect(Collectors.toList());


        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

        var all = store.findAll(query).collect(Collectors.toList());
        assertThat(all).hasSize(10);
    }


    @Override
    protected PolicyDefinitionStore getPolicyDefinitionStore() {
        return store;
    }

    @Override
    protected boolean supportCollectionQuery() {
        return true;
    }

    @Override
    protected boolean supportCollectionIndexQuery() {
        return true;
    }

    @Override
    protected Boolean supportSortOrder() {
        return true;
    }

    private PolicyDefinition convert(Object object) {
        var json = typeManager.writeValueAsString(object);
        return typeManager.readValue(json, PolicyDocument.class).getWrappedInstance();
    }
}
