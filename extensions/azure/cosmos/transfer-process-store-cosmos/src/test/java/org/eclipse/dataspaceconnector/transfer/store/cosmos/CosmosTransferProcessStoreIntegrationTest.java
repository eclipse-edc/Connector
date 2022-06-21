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

package org.eclipse.dataspaceconnector.transfer.store.cosmos;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.implementation.BadRequestException;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosStoredProcedureRequestOptions;
import com.azure.cosmos.models.CosmosStoredProcedureResponse;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.dataspaceconnector.azure.testfixtures.CosmosTestClient;
import org.eclipse.dataspaceconnector.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.transfer.store.cosmos.model.TransferProcessDocument;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.transfer.store.cosmos.TestHelper.createTransferProcess;
import static org.eclipse.dataspaceconnector.transfer.store.cosmos.TestHelper.createTransferProcessDocument;

@AzureCosmosDbIntegrationTest
class CosmosTransferProcessStoreIntegrationTest {

    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String DATABASE_NAME = "transferprocessstore-itest_" + TEST_ID;
    private static final String CONTAINER_PREFIX = "container_";
    private static CosmosContainer container;
    private static CosmosDatabase database;
    private final Clock clock = Clock.systemUTC();
    private final String partitionKey = "testpartition";
    private final String connectorId = "test-connector";
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

        var retryPolicy = new RetryPolicy<>().withMaxRetries(5).withBackoff(1, 3, ChronoUnit.SECONDS);
        var cosmosDbApi = new CosmosDbApiImpl(container, false);
        cosmosDbApi.uploadStoredProcedure("nextForState");
        cosmosDbApi.uploadStoredProcedure("lease");
        store = new CosmosTransferProcessStore(cosmosDbApi, typeManager, partitionKey, connectorId, retryPolicy);
    }

    @AfterEach
    void tearDown() {
        container.delete();
    }

    @Test
    void create() {
        String id = UUID.randomUUID().toString();
        TransferProcess transferProcess = createTransferProcess(id);
        store.create(transferProcess);

        CosmosPagedIterable<Object> documents = container.readAllItems(new PartitionKey(partitionKey), Object.class);
        assertThat(documents).hasSize(1);
        assertThat(documents).allSatisfy(obj -> {
            var doc = convert(obj);
            assertThat(doc.getWrappedInstance()).isNotNull();
            assertThat(doc.getWrappedInstance().getId()).isEqualTo(id);
            assertThat(doc.getPartitionKey()).isEqualTo(partitionKey);
            assertThat(doc.getLease()).isNull();
        });
    }

    @Test
    void create_processWithSameIdExists_shouldReplace() {
        String id = UUID.randomUUID().toString();
        TransferProcess transferProcess = createTransferProcess(id);
        store.create(transferProcess);

        var secondProcess = createTransferProcess(id);

        store.create(secondProcess); //should not throw
        assertThat(container.readAllItems(new PartitionKey(partitionKey), Object.class)).hasSize(1);
    }

    @Test
    void nextForState_fetchMaxNewest() throws InterruptedException {

        String id1 = UUID.randomUUID().toString();
        var tp = createTransferProcess(id1, TransferProcessStates.INITIAL);

        String id2 = UUID.randomUUID().toString();
        var tp2 = createTransferProcess(id2, TransferProcessStates.INITIAL);

        Thread.sleep(500); //make sure the third process is the youngest - should not get fetched
        String id3 = UUID.randomUUID().toString();
        var tp3 = createTransferProcess(id3, TransferProcessStates.INITIAL);

        store.create(tp);
        store.create(tp2);
        store.create(tp3);


        List<TransferProcess> processes = store.nextForState(TransferProcessStates.INITIAL.code(), 2);

        assertThat(processes).hasSize(2)
                .allMatch(p -> Arrays.asList(id1, id2).contains(p.getId()))
                .noneMatch(p -> p.getId().equals(id3));
    }

    @Test
    void nextForState_leaseByAnotherHolderExpired() {
        String id1 = UUID.randomUUID().toString();
        var tp = createTransferProcess(id1, TransferProcessStates.INITIAL);
        TransferProcessDocument item = new TransferProcessDocument(tp, partitionKey);
        Duration leaseDuration = Duration.ofSeconds(10);
        item.acquireLease("another-connector", clock, leaseDuration);
        container.upsertItem(item);

        List<TransferProcess> processesBeforeLeaseBreak = store.nextForState(TransferProcessStates.INITIAL.code(), 10);
        assertThat(processesBeforeLeaseBreak).isEmpty();

        await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(500))
                .pollDelay(leaseDuration) //give the lease time to expire
                .untilAsserted(() -> {
                    List<TransferProcess> processesAfterLeaseBreak = store.nextForState(TransferProcessStates.INITIAL.code(), 10);
                    assertThat(processesAfterLeaseBreak).hasSize(1).allSatisfy(transferProcess -> assertThat(transferProcess).usingRecursiveComparison().isEqualTo(tp));
                });
    }

    @Test
    void nextForState_shouldOnlyReturnFreeItems() {
        String id1 = "process1";
        var tp = createTransferProcess(id1, TransferProcessStates.INITIAL);

        String id2 = "process2";
        var tp2 = createTransferProcess(id2, TransferProcessStates.INITIAL);

        store.create(tp);
        store.create(tp2);
        TransferProcessDocument item = readDocument(id2);
        item.acquireLease("test-leaser", clock);
        container.upsertItem(item);

        //act - one should be ignored
        List<TransferProcess> processes = store.nextForState(TransferProcessStates.INITIAL.code(), 5);
        assertThat(processes).hasSize(1)
                .allMatch(p -> p.getId().equals(id1));
    }

    @Test
    void nextForState_selfCannotLeaseAgain() {
        var tp1 = createTransferProcess("process1", TransferProcessStates.INITIAL);
        var doc = new TransferProcessDocument(tp1, partitionKey);
        doc.acquireLease(connectorId, clock);
        var originalTimestamp = doc.getLease().getLeasedAt();
        container.upsertItem(doc);

        var result = store.nextForState(TransferProcessStates.INITIAL.code(), 5);
        assertThat(result).isEmpty();

        var updatedDoc = readDocument(tp1.getId());
        assertThat(updatedDoc.getLease().getLeasedAt()).isEqualTo(originalTimestamp);
        assertThat(doc.getLease().getLeasedBy()).isEqualTo(connectorId);
        assertThat(doc.getLease().getLeaseDuration()).isEqualTo(60000L);

    }

    @Test
    void nextForState_noFreeItem_shouldReturnEmpty() {
        String id1 = "process1";
        var tp = createTransferProcess(id1, TransferProcessStates.INITIAL);

        String id2 = "process2";
        var tp2 = createTransferProcess(id2, TransferProcessStates.INITIAL);

        var d1 = new TransferProcessDocument(tp, partitionKey);
        d1.acquireLease("another-connector", clock);
        var d2 = new TransferProcessDocument(tp2, partitionKey);
        d2.acquireLease("a-third-connector", clock);

        container.upsertItem(d1);
        container.upsertItem(d2);

        assertThat(store.nextForState(TransferProcessStates.INITIAL.code(), 5)).isEmpty();
    }

    @Test
    void nextForState_noneInDesiredState() {

        String id1 = UUID.randomUUID().toString();
        var tp = createTransferProcess(id1, TransferProcessStates.UNSAVED);

        String id2 = UUID.randomUUID().toString();
        var tp2 = createTransferProcess(id2, TransferProcessStates.UNSAVED);

        String id3 = UUID.randomUUID().toString();
        var tp3 = createTransferProcess(id3, TransferProcessStates.UNSAVED);

        store.create(tp);
        store.create(tp2);
        store.create(tp3);

        List<TransferProcess> processes = store.nextForState(TransferProcessStates.IN_PROGRESS.code(), 5);

        assertThat(processes).isEmpty();
    }

    @Test
    void nextForState_batchSizeLimits() {
        for (var i = 0; i < 5; i++) {
            var tp = createTransferProcess("process_" + i, TransferProcessStates.INITIAL);
            store.create(tp);
        }

        var processes = store.nextForState(TransferProcessStates.INITIAL.code(), 3);
        assertThat(processes).hasSize(3);
    }

    @Test
    @DisplayName("Verifies that calling nextForState locks the TP for any subsequent calls")
    void nextForState_locksEntity() {
        var tp = createTransferProcess("test-id", TransferProcessStates.IN_PROGRESS);
        var doc = new TransferProcessDocument(tp, partitionKey);

        container.upsertItem(doc);
        var result = store.nextForState(TransferProcessStates.IN_PROGRESS.code(), 5);
        assertThat(result).hasSize(1).containsExactly(tp);

        //make sure the lease is acquired
        var leasedDoc = readDocument(tp.getId());
        assertThat(leasedDoc.getLease()).isNotNull();

        assertThat(leasedDoc.getLease().getLeasedBy()).isEqualTo(connectorId);
        assertThat(leasedDoc.getLease().getLeaseDuration()).isEqualTo(60_000L);
        assertThat(leasedDoc.getLease().getLeasedAt()).isGreaterThan(0);

        //make sure a subsequent call does not return the TP
        assertThat(store.nextForState(TransferProcessStates.IN_PROGRESS.code(), 5)).isEmpty();

        //make sure that findById still returns the entity
        assertThat(store.find(tp.getId())).isEqualTo(tp);
    }

    @Test
    @DisplayName("Verify that the lease on a TP is cleared by an update")
    void nextForState_verifyUpdateClearsLease() {
        var tp = createTransferProcess("test-id", TransferProcessStates.IN_PROGRESS);
        var doc = new TransferProcessDocument(tp, partitionKey);

        var initialTimestamp = tp.getStateTimestamp();

        container.upsertItem(doc);
        var result = store.nextForState(TransferProcessStates.IN_PROGRESS.code(), 5);
        assertThat(result).hasSize(1);

        //make sure the lease is acquired
        var leasedDoc = readDocument(tp.getId());
        assertThat(leasedDoc.getLease()).isNotNull();

        // make sure the next state update clears the lease
        tp.transitionCompleted();
        tp.updateStateTimestamp();
        store.update(tp);

        var updatedDocument = readDocument(tp.getId());
        assertThat(updatedDocument.getLease()).isNull();
        assertThat(updatedDocument.getWrappedInstance().getStateTimestamp()).isNotEqualTo(initialTimestamp);
        assertThat(updatedDocument.getWrappedInstance().getState()).isEqualTo(TransferProcessStates.COMPLETED.code());
    }

    @Test
    @DisplayName("Verify that a leased entity can still be deleted")
    void nextForState_verifyDelete() {
        var tp = createTransferProcess("test-id", TransferProcessStates.IN_PROGRESS);
        var doc = new TransferProcessDocument(tp, partitionKey);

        container.upsertItem(doc);
        var result = store.nextForState(TransferProcessStates.IN_PROGRESS.code(), 5);
        assertThat(result).hasSize(1);

        //make sure the lease is acquired
        var leasedDoc = readDocument(tp.getId());
        assertThat(leasedDoc.getLease()).isNotNull();

        store.delete(tp.getId());
        assertThat(container.readAllItems(new PartitionKey(partitionKey), Object.class)).isEmpty();
    }

    @Test
    void find() {
        var tp = createTransferProcess("tp-id");
        store.create(tp);

        TransferProcess dbProcess = store.find("tp-id");
        assertThat(dbProcess).isNotNull().isEqualTo(tp).usingRecursiveComparison();
    }

    @Test
    void find_notExist() {
        assertThat(store.find("not-exist")).isNull();
    }

    @Test
    void processIdForTransferId() {
        var tp = createTransferProcess("process-id");
        var transferId = tp.getDataRequest().getId();
        assertThat(transferId).isNotNull();

        store.create(tp);

        String processId = store.processIdForTransferId(transferId);
        assertThat(processId).isEqualTo("process-id");
    }

    @Test
    void processIdForTransferId_notExist() {
        assertThat(store.processIdForTransferId("not-exist")).isNull();
    }

    @Test
    void update_exists_shouldUpdate() {
        var tp = createTransferProcess("process-id");

        store.create(tp);

        tp.transitionProvisioning(ResourceManifest.Builder.newInstance().build());
        store.update(tp);

        CosmosItemResponse<Object> response = container.readItem(tp.getId(), new PartitionKey(partitionKey), Object.class);

        TransferProcessDocument stored = convert(response.getItem());
        assertThat(stored.getWrappedInstance()).isEqualTo(tp);
        assertThat(stored.getWrappedInstance().getState()).isEqualTo(TransferProcessStates.PROVISIONING.code());
        assertThat(stored.getLease()).isNull();
    }

    @Test
    void update_notExist_shouldCreate() {
        var tp = createTransferProcess("process-id");

        tp.transitionProvisioning(ResourceManifest.Builder.newInstance().build());
        store.update(tp);

        var response = container.readItem(tp.getId(), new PartitionKey(partitionKey), Object.class);

        var stored = convert(response.getItem());
        assertThat(stored.getWrappedInstance()).isEqualTo(tp);
        assertThat(stored.getWrappedInstance().getState()).isEqualTo(TransferProcessStates.PROVISIONING.code());
        assertThat(stored.getLease()).isNull();
    }

    @Test
    void update_leasedBySelf() {
        var tp = createTransferProcess("proc1", TransferProcessStates.INITIAL);

        var doc = new TransferProcessDocument(tp, partitionKey);
        container.upsertItem(doc).getItem();
        doc.acquireLease(connectorId, clock);
        container.upsertItem(doc);

        tp.transitionProvisioning(ResourceManifest.Builder.newInstance().build());
        store.update(tp);

        assertThat(tp.getState()).isEqualTo(TransferProcessStates.PROVISIONING.code());
    }

    @Test
    void update_leasedByOther_shouldThrowException() {
        var tp = createTransferProcess("proc1", TransferProcessStates.INITIAL);

        //simulate another connector
        var doc = new TransferProcessDocument(tp, partitionKey);
        container.upsertItem(doc).getItem();

        doc.acquireLease("another-connector", clock);
        container.upsertItem(doc);

        //act
        tp.transitionProvisioning(ResourceManifest.Builder.newInstance().build());
        assertThatThrownBy(() -> store.update(tp)).isInstanceOf(EdcException.class).hasRootCauseInstanceOf(BadRequestException.class);
    }

    @Test
    void delete() {

        final String processId = "test-process-id";
        var tp = createTransferProcess(processId);

        store.create(tp);

        store.delete(processId);

        CosmosPagedIterable<Object> objects = container.readAllItems(new PartitionKey(processId), Object.class);
        assertThat(objects).isEmpty();
    }

    @Test
    void delete_isLeased_shouldThrowException() {
        final String processId = "test-process-id";
        var tp = createTransferProcess(processId);
        var doc = new TransferProcessDocument(tp, partitionKey);
        doc.acquireLease("some-other-connector", clock);
        container.upsertItem(doc);

        assertThatThrownBy(() -> store.delete(processId)).isInstanceOf(EdcException.class).hasRootCauseInstanceOf(BadRequestException.class);
    }

    @Test
    void delete_isLeasedBySelf() {
        final String processId = "test-process-id";
        var tp = createTransferProcess(processId);
        var doc = new TransferProcessDocument(tp, partitionKey);
        doc.acquireLease(connectorId, clock);
        container.upsertItem(doc);

        store.delete(processId);
    }

    @Test
    void delete_notExist() {
        store.delete("not-exist");
        //no exception should be raised
    }

    @Test
    void invokeStoredProcedure() {
        //create one item
        var tp = createTransferProcess("proc1");
        store.create(tp);

        List<Object> procedureParams = Arrays.asList(100, 5, connectorId);
        CosmosStoredProcedureRequestOptions options = new CosmosStoredProcedureRequestOptions();
        options.setPartitionKey(PartitionKey.NONE);
        CosmosStoredProcedureResponse sprocResponse = container.getScripts().getStoredProcedure("nextForState").execute(procedureParams, options);

        var result = sprocResponse.getResponseAsString();

        var l = typeManager.readValue(result, List.class);
        List<TransferProcessDocument> documents = (List<TransferProcessDocument>) l.stream().map(o -> typeManager.writeValueAsString(o))
                .map(json -> typeManager.readValue(json.toString(), TransferProcessDocument.class))
                .collect(Collectors.toList());
        assertThat(documents).allSatisfy(document -> {
            assertThat(document.getLease()).isNotNull().hasFieldOrPropertyWithValue("leasedBy", connectorId)
                    .hasFieldOrProperty("leasedAt");
        });

    }

    @Test
    void findAll_noQuerySpec() {
        var doc1 = createTransferProcessDocument("tp1", partitionKey);
        var doc2 = createTransferProcessDocument("tp2", partitionKey);

        container.createItem(doc1);
        container.createItem(doc2);

        assertThat(store.findAll(QuerySpec.none())).hasSize(2).extracting(TransferProcess::getId).containsExactlyInAnyOrder(doc1.getId(), doc2.getId());
    }

    @Test
    void findAll_verifyPaging() {

        var all = IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcessDocument("tp" + i, partitionKey))
                .peek(d -> container.createItem(d))
                .map(TransferProcessDocument::getId)
                .collect(Collectors.toList());

        // page size fits
        assertThat(store.findAll(QuerySpec.Builder.newInstance().offset(3).limit(4).build())).hasSize(4).extracting(TransferProcess::getId).isSubsetOf(all);

    }

    @Test
    void findAll_verifyPaging_pageSizeLargerThanCollection() {

        var all = IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcessDocument("tp" + i, partitionKey))
                .peek(d -> container.createItem(d)).map(TransferProcessDocument::getId)
                .collect(Collectors.toList());

        // page size fits
        assertThat(store.findAll(QuerySpec.Builder.newInstance().offset(3).limit(40).build())).hasSize(7).extracting(TransferProcess::getId).isSubsetOf(all);
    }

    @Test
    void findAll_verifyFiltering() {
        var documents = IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcessDocument("tp" + i, partitionKey))
                .peek(d -> container.createItem(d))
                .collect(Collectors.toList());

        var expectedId = documents.get(3).getId();

        var query = QuerySpec.Builder.newInstance().filter("id=" + expectedId).build();
        assertThat(store.findAll(query)).extracting(TransferProcess::getId).containsOnly(expectedId);
    }

    @Test
    void findAll_verifyFiltering_invalidFilterExpression() {
        IntStream.range(0, 10).mapToObj(i -> createTransferProcessDocument("tp" + i, partitionKey)).forEach(d -> container.createItem(d));

        var query = QuerySpec.Builder.newInstance().filter("something contains other").build();

        assertThatThrownBy(() -> store.findAll(query)).isInstanceOfAny(IllegalArgumentException.class).hasMessage("Cannot build WHERE clause, reason: unsupported operator contains");
    }

    @Test
    void findAll_verifyFiltering_unsuccessfulFilterExpression() {
        IntStream.range(0, 10).mapToObj(i -> createTransferProcessDocument("tp" + i, partitionKey)).forEach(d -> container.createItem(d));

        var query = QuerySpec.Builder.newInstance().filter("something = other").build();

        assertThat(store.findAll(query)).isEmpty();
    }

    @Test
    void findAll_verifySorting() {

        IntStream.range(0, 10).mapToObj(i -> createTransferProcessDocument("tp" + i, partitionKey)).forEach(d -> container.createItem(d));

        var ascendingQuery = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build();
        assertThat(store.findAll(ascendingQuery)).hasSize(10).isSortedAccordingTo(Comparator.comparing(TransferProcess::getId));
        var descendingQuery = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build();
        assertThat(store.findAll(descendingQuery)).hasSize(10).isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));
    }

    private TransferProcessDocument convert(Object obj) {
        return typeManager.readValue(typeManager.writeValueAsBytes(obj), TransferProcessDocument.class);
    }

    private TransferProcessDocument readDocument(String id) {
        CosmosItemResponse<Object> response = container.readItem(id, new PartitionKey(partitionKey), Object.class);
        return convert(response.getItem());
    }
}