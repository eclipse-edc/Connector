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

package org.eclipse.dataspaceconnector.transfer.store.memory;

import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.INITIAL;
import static org.eclipse.dataspaceconnector.transfer.store.memory.TestFunctions.createProcess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryTransferProcessStoreTest {
    private InMemoryTransferProcessStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryTransferProcessStore();
    }

    @Test
    void verifyCreateUpdateDelete() {
        String id = UUID.randomUUID().toString();
        TransferProcess transferProcess = TransferProcess.Builder.newInstance().id(id).dataRequest(DataRequest.Builder.newInstance().id("clientid").destinationType("test").build()).build();
        transferProcess.transitionInitial();
        store.create(transferProcess);

        TransferProcess found = store.find(id);

        assertNotNull(found);
        assertNotSame(found, transferProcess); // enforce by-value

        assertNotNull(store.processIdForTransferId("clientid"));

        assertEquals(INITIAL.code(), found.getState());

        transferProcess.transitionProvisioning(ResourceManifest.Builder.newInstance().build());

        store.update(transferProcess);

        found = store.find(id);
        assertNotNull(found);
        assertEquals(TransferProcessStates.PROVISIONING.code(), found.getState());

        store.delete(id);
        Assertions.assertNull(store.find(id));
        assertNull(store.processIdForTransferId("clientid"));

    }

    @Test
    void verifyNext() throws InterruptedException {
        var transferProcess1 = initialTransferProcess();
        store.create(transferProcess1);
        var transferProcess2 = initialTransferProcess();
        store.create(transferProcess2);

        transferProcess2.transitionProvisioning(ResourceManifest.Builder.newInstance().build());
        store.update(transferProcess2);
        Thread.sleep(1);
        transferProcess1.transitionProvisioning(ResourceManifest.Builder.newInstance().build());
        store.update(transferProcess1);

        assertThat(store.nextForState(INITIAL.code(), 1)).isEmpty();

        var found = store.nextForState(TransferProcessStates.PROVISIONING.code(), 1);
        assertThat(found).hasSize(1).first().matches(it -> it.equals(transferProcess2));

        found = store.nextForState(TransferProcessStates.PROVISIONING.code(), 3);
        assertThat(found).hasSize(1).first().matches(it -> it.equals(transferProcess1));
    }

    @Test
    void nextForState_shouldLeaseEntityUntilUpdate() {
        var initialTransferProcess = initialTransferProcess();
        store.create(initialTransferProcess);

        var firstQueryResult = store.nextForState(INITIAL.code(), 1);
        assertThat(firstQueryResult).hasSize(1);

        var secondQueryResult = store.nextForState(INITIAL.code(), 1);
        assertThat(secondQueryResult).hasSize(0);

        var retrieved = firstQueryResult.get(0);
        store.update(retrieved);

        var thirdQueryResult = store.nextForState(INITIAL.code(), 1);
        assertThat(thirdQueryResult).hasSize(1);
    }

    @Test
    void verifyMutlipleRequets() {
        String id1 = UUID.randomUUID().toString();
        TransferProcess transferProcess1 = TransferProcess.Builder.newInstance().id(id1).dataRequest(DataRequest.Builder.newInstance().id("clientid1").destinationType("test").build()).build();
        transferProcess1.transitionInitial();
        store.create(transferProcess1);

        String id2 = UUID.randomUUID().toString();
        TransferProcess transferProcess2 = TransferProcess.Builder.newInstance().id(id2).dataRequest(DataRequest.Builder.newInstance().id("clientid2").destinationType("test").build()).build();
        transferProcess2.transitionInitial();
        store.create(transferProcess2);


        TransferProcess found1 = store.find(id1);
        assertNotNull(found1);

        TransferProcess found2 = store.find(id2);
        assertNotNull(found2);

        var found = store.nextForState(INITIAL.code(), 3);
        assertEquals(2, found.size());
    }

    @Test
    void verifyOrderingByTimestamp() {
        for (int i = 0; i < 100; i++) {
            TransferProcess process = createProcess("test-process-" + i);
            process.transitionInitial();
            store.create(process);
        }

        List<TransferProcess> processes = store.nextForState(INITIAL.code(), 50);

        assertThat(processes).hasSize(50);
        assertThat(processes).allMatch(p -> p.getStateTimestamp() > 0);
    }

    @Test
    void verifyNextForState_avoidsStarvation() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            TransferProcess process = createProcess("test-process-" + i);
            process.transitionInitial();
            store.create(process);
        }

        var list1 = store.nextForState(INITIAL.code(), 5);
        Thread.sleep(50); //simulate a short delay to generate different timestamps
        list1.forEach(tp -> store.update(tp));
        var list2 = store.nextForState(INITIAL.code(), 5);
        assertThat(list1).isNotEqualTo(list2).doesNotContainAnyElementsOf(list2);
    }

    @Test
    void findAll_noQuerySpec() {
        IntStream.range(0, 10).forEach(i -> store.create(createProcess("test-neg-" + i)));

        var all = store.findAll(QuerySpec.Builder.newInstance().build());
        assertThat(all).hasSize(10);
    }

    @Test
    void findAll_verifyPaging() {

        IntStream.range(0, 10).forEach(i -> store.create(createProcess("test-neg-" + i)));

        // page size fits
        assertThat(store.findAll(QuerySpec.Builder.newInstance().offset(3).limit(4).build())).hasSize(4);

        // page size too large
        assertThat(store.findAll(QuerySpec.Builder.newInstance().offset(5).limit(100).build())).hasSize(5);
    }

    @Test
    void findAll_verifyFiltering() {
        IntStream.range(0, 10).forEach(i -> store.create(createProcess("test-neg-" + i)));
        assertThat(store.findAll(QuerySpec.Builder.newInstance().equalsAsContains(false).filter("id=test-neg-3").build())).extracting(TransferProcess::getId).containsOnly("test-neg-3");
    }

    @Test
    void findAll_verifyFiltering_invalidFilterExpression() {
        IntStream.range(0, 10).forEach(i -> store.create(createProcess("test-neg-" + i)));
        assertThatThrownBy(() -> store.findAll(QuerySpec.Builder.newInstance().filter("something foobar other").build())).isInstanceOfAny(IllegalArgumentException.class);
    }

    @Test
    void findAll_verifySorting() {
        IntStream.range(0, 10).forEach(i -> store.create(createProcess("test-neg-" + i)));

        assertThat(store.findAll(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build())).hasSize(10).isSortedAccordingTo(Comparator.comparing(TransferProcess::getId));
        assertThat(store.findAll(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build())).hasSize(10).isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));
    }

    @Test
    void findAll_verifySorting_invalidProperty() {
        IntStream.range(0, 10).forEach(i -> store.create(createProcess("test-neg-" + i)));

        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

        assertThat(store.findAll(query).collect(Collectors.toList())).isEmpty();
    }

    @NotNull
    private TransferProcess initialTransferProcess() {
        var process = TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString()).dataRequest(DataRequest.Builder.newInstance().id("clientid").destinationType("test").build()).build();
        process.transitionInitial();
        return process;
    }
}
