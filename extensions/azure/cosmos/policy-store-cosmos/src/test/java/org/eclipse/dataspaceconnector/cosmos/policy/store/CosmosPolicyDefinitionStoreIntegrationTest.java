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

package org.eclipse.dataspaceconnector.cosmos.policy.store;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.azure.cosmos.models.PartitionKey;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.dataspaceconnector.azure.testfixtures.CosmosTestClient;
import org.eclipse.dataspaceconnector.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.cosmos.policy.store.TestFunctions.generateDocument;
import static org.eclipse.dataspaceconnector.cosmos.policy.store.TestFunctions.generatePolicy;

@AzureCosmosDbIntegrationTest
public class CosmosPolicyDefinitionStoreIntegrationTest {
    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String DATABASE_NAME = "connector-itest-" + TEST_ID;
    private static final String CONTAINER_PREFIX = "ContractDefinitionStore-";
    private static final String TEST_PARTITION_KEY = "test-part-key";
    private static CosmosContainer container;
    private static CosmosDatabase database;
    private static TypeManager typeManager;
    private CosmosPolicyDefinitionStore store;

    @BeforeAll
    static void prepareCosmosClient() {
        var client = CosmosTestClient.createClient();
        typeManager = new TypeManager();

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
        store = new CosmosPolicyDefinitionStore(cosmosDbApi, typeManager, new RetryPolicy<>().withMaxRetries(3).withBackoff(1, 5, ChronoUnit.SECONDS), TEST_PARTITION_KEY);
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
        store.save(policy);

        var actual = container.readAllItems(new PartitionKey(TEST_PARTITION_KEY), Object.class);
        assertThat(actual).hasSize(1);
        var doc = actual.stream().findFirst().get();
        assertThat(convert(doc)).isEqualTo(policy);
    }

    @Test
    void save_exists_shouldUpdate() {
        var doc1 = generateDocument(TEST_PARTITION_KEY);
        container.createItem(doc1);

        var policyToUpdate = doc1.getWrappedInstance();

        //modify a single field
        policyToUpdate.getPolicy().getPermissions().add(Permission.Builder.newInstance().target("test-permission-target").build());
        policyToUpdate.getPolicy().getObligations().add(Duty.Builder.newInstance().uid("test-obligation-id").build());


        store.save(policyToUpdate);
        var actual = container.readAllItems(new PartitionKey(doc1.getPartitionKey()), Object.class);
        assertThat(actual).hasSize(1);

        var first = convert(actual.stream().findFirst().get());

        assertThat(first.getPolicy().getPermissions()).hasSize(1).extracting(Permission::getTarget).containsOnly("test-permission-target");
        assertThat(first.getPolicy().getObligations()).hasSize(1).extracting(Duty::getUid).containsOnly("test-obligation-id");

    }

    @Test
    void delete() {
        var document = generateDocument(TEST_PARTITION_KEY);
        container.createItem(document);

        var policy = convert(document);
        var deletedPolicy = store.deleteById(document.getId());
        assertThat(deletedPolicy).isEqualTo(policy);

        assertThat(container.readAllItems(new PartitionKey(document.getPartitionKey()), Object.class)).isEmpty();
    }

    @Test
    void delete_notExist() {
        assertThatThrownBy(() -> store.deleteById("not-exists"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("An object with the ID not-exists could not be found!");
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

        var query = QuerySpec.Builder.newInstance().filter("uid=" + expectedId).build();
        assertThat(store.findAll(query)).extracting(PolicyDefinition::getUid).containsOnly(expectedId);
    }

    @Test
    void findAll_verifyFiltering_invalidFilterExpression() {
        IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PARTITION_KEY)).forEach(d -> container.createItem(d));

        var query = QuerySpec.Builder.newInstance().filter("something contains other").build();

        // message is coming from the predicate converter rather than the SQL statement translation layer
        assertThatThrownBy(() -> store.findAll(query)).isInstanceOfAny(IllegalArgumentException.class).hasMessage("Operator [contains] is not supported by this converter!");
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

        var ascendingQuery = QuerySpec.Builder.newInstance().sortField("uid").sortOrder(SortOrder.ASC).build();
        assertThat(store.findAll(ascendingQuery)).hasSize(10).isSortedAccordingTo(Comparator.comparing(PolicyDefinition::getUid));
        var descendingQuery = QuerySpec.Builder.newInstance().sortField("uid").sortOrder(SortOrder.DESC).build();
        assertThat(store.findAll(descendingQuery)).hasSize(10).isSortedAccordingTo((c1, c2) -> c2.getUid().compareTo(c1.getUid()));
    }

    @Test
    void findAll_sorting_nonExistentProperty() {

        var ids = IntStream.range(0, 10).mapToObj(i -> generateDocument(TEST_PARTITION_KEY)).peek(d -> container.createItem(d)).map(PolicyDocument::getId).collect(Collectors.toList());


        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

        var all = store.findAll(query).collect(Collectors.toList());
        assertThat(all).isEmpty();
    }

    @Test
    void verify_readWriteFindAll() {
        // add an object
        var policy = generatePolicy();
        store.save(policy);
        assertThat(store.findAll(QuerySpec.none())).containsExactly(policy);

        // modify the object
        var modifiedPolicy = PolicyDefinition.Builder.newInstance()
                .policy(Policy.Builder.newInstance()

                        .permission(Permission.Builder.newInstance()
                                .target("test-asset-id")
                                .action(Action.Builder.newInstance()
                                        .type("USE")
                                        .build())
                                .build())
                        .build())
                .uid(policy.getUid())
                .build();

        store.save(modifiedPolicy);

        // re-read
        var all = store.findAll(QuerySpec.Builder.newInstance().filter("policy.permissions[0].target=test-asset-id").build()).collect(Collectors.toList());
        assertThat(all).hasSize(1).containsExactly(modifiedPolicy);

    }

    private PolicyDefinition convert(Object object) {
        var json = typeManager.writeValueAsString(object);
        return typeManager.readValue(json, PolicyDocument.class).getWrappedInstance();
    }
}
