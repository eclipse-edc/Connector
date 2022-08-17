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

package org.eclipse.dataspaceconnector.sql.transferprocess.store;

import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.sql.lease.LeaseUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.sql.transferprocess.store.TestFunctions.createDataRequest;
import static org.eclipse.dataspaceconnector.sql.transferprocess.store.TestFunctions.createDataRequestBuilder;
import static org.eclipse.dataspaceconnector.sql.transferprocess.store.TestFunctions.createTransferProcess;
import static org.eclipse.dataspaceconnector.sql.transferprocess.store.TestFunctions.createTransferProcessBuilder;
import static org.hamcrest.Matchers.hasSize;

abstract class TransferProcessStoreTest {
    protected static final String CONNECTOR_NAME = "test-connector";
    protected SqlTransferProcessStore store;
    protected LeaseUtil leaseUtil;

    @Test
    void create() {
        var t = createTransferProcess("test-id");
        store.create(t);

        var all = store.findAll(QuerySpec.none()).collect(Collectors.toList());
        assertThat(all).containsExactly(t);
        assertThat(all.get(0)).usingRecursiveComparison().isEqualTo(t);
        assertThat(all).allSatisfy(tr -> assertThat(tr.getCreatedAt()).isNotEqualTo(0L));
    }

    @Test
    void create_withSameIdExists_shouldReplace() {
        var t = createTransferProcess("id1", TransferProcessStates.UNSAVED);
        store.create(t);

        var t2 = createTransferProcess("id1", TransferProcessStates.PROVISIONING);
        store.create(t2);

        assertThat(store.findAll(QuerySpec.none())).hasSize(1).containsExactly(t2);
    }

    @Test
    void nextForState() {
        var state = TransferProcessStates.IN_PROGRESS;
        var all = IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .peek(store::create)
                .collect(Collectors.toList());

        assertThat(store.nextForState(state.code(), 5))
                .hasSize(5)
                .extracting(TransferProcess::getId)
                .isSubsetOf(all.stream().map(TransferProcess::getId).collect(Collectors.toList()))
                .allMatch(id -> leaseUtil.isLeased(id, CONNECTOR_NAME));
    }

    @Test
    void nextForState_shouldOnlyReturnFreeItems() {
        var state = TransferProcessStates.IN_PROGRESS;
        var all = IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .peek(store::create)
                .collect(Collectors.toList());

        // lease a few
        var leasedTp = all.stream().skip(5).peek(tp -> leaseUtil.leaseEntity(tp.getId(), CONNECTOR_NAME)).collect(Collectors.toList());

        // should not contain leased TPs
        assertThat(store.nextForState(state.code(), 10))
                .hasSize(5)
                .isSubsetOf(all)
                .doesNotContainAnyElementsOf(leasedTp);
    }

    @Test
    void nextForState_noFreeItem_shouldReturnEmpty() {
        var state = TransferProcessStates.IN_PROGRESS;
        IntStream.range(0, 3)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .forEach(store::create);

        // first time works
        assertThat(store.nextForState(state.code(), 10)).hasSize(3);
        // second time returns empty list
        assertThat(store.nextForState(state.code(), 10)).isEmpty();
    }

    @Test
    void nextForState_noneInDesiredState() {
        var state = TransferProcessStates.IN_PROGRESS;
        IntStream.range(0, 3)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .forEach(store::create);

        assertThat(store.nextForState(TransferProcessStates.CANCELLED.code(), 10)).isEmpty();
    }

    @Test
    void nextForState_batchSizeLimits() {
        var state = TransferProcessStates.IN_PROGRESS;
        IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .forEach(store::create);

        // first time works
        assertThat(store.nextForState(state.code(), 3)).hasSize(3);
    }

    @Test
    void nextForState_verifyTemporalOrdering() {
        var state = TransferProcessStates.IN_PROGRESS;
        IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess(String.valueOf(i), state))
                .peek(t -> {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                        // noop
                    }
                    t.updateStateTimestamp();
                })
                .forEach(store::create);

        assertThat(store.nextForState(state.code(), 20))
                .extracting(TransferProcess::getId)
                .map(Integer::parseInt)
                .isSortedAccordingTo(Integer::compareTo);
    }

    @Test
    void nextForState_verifyMostRecentlyUpdatedIsLast() throws InterruptedException {
        var state = TransferProcessStates.IN_PROGRESS;
        var all = IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .peek(store::create)
                .collect(Collectors.toList());

        Thread.sleep(100);

        var fourth = all.get(3);
        fourth.updateStateTimestamp();
        store.update(fourth);

        var next = store.nextForState(TransferProcessStates.IN_PROGRESS.code(), 20);
        assertThat(next.indexOf(fourth)).isEqualTo(9);
    }

    @Test
    @DisplayName("Verifies that calling nextForState locks the TP for any subsequent calls")
    void nextForState_locksEntity() {
        var t = createTransferProcess("id1", TransferProcessStates.UNSAVED);
        store.create(t);

        store.nextForState(TransferProcessStates.UNSAVED.code(), 100);

        assertThat(leaseUtil.isLeased(t.getId(), CONNECTOR_NAME)).isTrue();
    }

    @Test
    void nextForState_expiredLease() {
        var t = createTransferProcess("id1", TransferProcessStates.UNSAVED);
        store.create(t);

        leaseUtil.leaseEntity(t.getId(), CONNECTOR_NAME, Duration.ofMillis(100));

        await().atLeast(Duration.ofMillis(100))
                .atMost(Duration.ofMillis(500))
                .until(() -> store.nextForState(TransferProcessStates.UNSAVED.code(), 10), hasSize(1));
    }

    @Test
    void find() {
        var t = createTransferProcess("id1");
        store.create(t);

        var res = store.find("id1");

        assertThat(res).usingRecursiveComparison().isEqualTo(t);
    }

    @Test
    void find_notExist() {
        assertThat(store.find("not-exist")).isNull();
    }

    @Test
    void processIdForTransferId() {
        var pid = "process-id1";
        var tid = "transfer-id1";

        var dr = createDataRequest(pid);
        var t = createTransferProcess(tid, dr);

        store.create(t);

        assertThat(store.processIdForTransferId(tid)).isEqualTo("transfer-id1");
    }

    @Test
    void processIdForTransferId_notExist() {
        assertThat(store.processIdForTransferId("not-exist")).isNull();
    }

    @Test
    void update_shouldPersistDataRequest() {
        var t1 = createTransferProcess("id1", TransferProcessStates.IN_PROGRESS);
        store.create(t1);

        t1.getDataRequest().getProperties().put("newKey", "newValue");
        store.update(t1);

        var all = store.findAll(QuerySpec.none()).collect(Collectors.toList());
        assertThat(all)
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(t1);

        assertThat(all.get(0).getDataRequest().getProperties()).containsEntry("newKey", "newValue");
    }


    @Test
    void update_exists_shouldUpdate() {
        var t1 = createTransferProcess("id1", TransferProcessStates.IN_PROGRESS);
        store.create(t1);

        t1.transitionCompleted(); //modify
        store.update(t1);

        assertThat(store.findAll(QuerySpec.none()))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(t1);
    }

    @Test
    void update_notExist_shouldCreate() {
        var t1 = createTransferProcess("id1", TransferProcessStates.IN_PROGRESS);

        t1.transitionCompleted(); //modify
        store.update(t1);

        var result = store.findAll(QuerySpec.none()).collect(Collectors.toList());
        assertThat(result)
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(t1);
    }

    @Test
    @DisplayName("Verify that the lease on a TP is cleared by an update")
    void update_shouldBreakLease() {
        var t1 = createTransferProcess("id1");
        store.create(t1);
        // acquire lease
        leaseUtil.leaseEntity(t1.getId(), CONNECTOR_NAME);

        t1.transitionInitial(); //modify
        store.update(t1);

        // lease should be broken
        assertThat(store.nextForState(TransferProcessStates.INITIAL.code(), 10)).usingRecursiveFieldByFieldElementComparator().containsExactly(t1);
    }

    @Test
    void update_leasedByOther_shouldThrowException() {

        var tpId = "id1";
        var t1 = createTransferProcess(tpId);
        store.create(t1);
        leaseUtil.leaseEntity(tpId, "someone");

        t1.transitionInitial(); //modify

        // leased by someone else -> throw exception
        assertThatThrownBy(() -> store.update(t1)).isInstanceOf(IllegalStateException.class);

    }

    @Test
    void delete() {
        var t1 = createTransferProcess("id1");
        store.create(t1);

        store.delete("id1");
        assertThat(store.findAll(QuerySpec.none())).isEmpty();
    }

    @Test
    void delete_isLeasedBySelf_shouldThrowException() {
        var t1 = createTransferProcess("id1");
        store.create(t1);
        leaseUtil.leaseEntity(t1.getId(), CONNECTOR_NAME);


        assertThatThrownBy(() -> store.delete("id1")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void delete_isLeasedByOther_shouldThrowException() {
        var t1 = createTransferProcess("id1");
        store.create(t1);

        leaseUtil.leaseEntity(t1.getId(), "someone-else");

        assertThatThrownBy(() -> store.delete("id1")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void delete_notExist() {
        store.delete("not-exist");
        //no exception should be raised
    }

    @Test
    void findAll_noQuerySpec() {
        var all = IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i))
                .peek(store::create)
                .collect(Collectors.toList());

        assertThat(store.findAll(QuerySpec.none())).containsExactlyInAnyOrderElementsOf(all);
    }

    @Test
    void findAll_verifyPaging() {
        IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess(String.valueOf(i)))
                .forEach(store::create);

        var qs = QuerySpec.Builder.newInstance().limit(5).offset(3).build();
        assertThat(store.findAll(qs)).hasSize(5)
                .extracting(TransferProcess::getId)
                .map(Integer::parseInt)
                .allMatch(id -> id >= 3 && id < 8);
    }

    @Test
    void findAll_verifyPaging_pageSizeLargerThanCollection() {

        IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess(String.valueOf(i)))
                .forEach(store::create);

        var qs = QuerySpec.Builder.newInstance().limit(20).offset(3).build();
        assertThat(store.findAll(qs))
                .hasSize(7)
                .extracting(TransferProcess::getId)
                .map(Integer::parseInt)
                .allMatch(id -> id >= 3 && id < 10);
    }

    @Test
    void findAll_verifyPaging_pageSizeOutsideCollection() {

        IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess(String.valueOf(i)))
                .forEach(store::create);

        var qs = QuerySpec.Builder.newInstance().limit(10).offset(12).build();
        assertThat(store.findAll(qs)).isEmpty();

    }

    @Test
    void create_withoutDataRequest_throwsException() {
        var t1 = createTransferProcessBuilder("id1")
                .dataRequest(null)
                .build();
        assertThatIllegalArgumentException().isThrownBy(() -> store.create(t1));
    }

    @Test
    void update_dataRequestWithNewId_replacesOld() {
        var bldr = createTransferProcessBuilder("id1").state(TransferProcessStates.IN_PROGRESS.code());
        var t1 = bldr.build();
        store.create(t1);

        var t2 = bldr
                .dataRequest(createDataRequestBuilder()
                        .id("new-dr-id")
                        .assetId("new-asset")
                        .contractId("new-contract")
                        .protocol("test-protocol")
                        .connectorId("new-connector")
                        .build())
                .build();
        store.update(t2);

        var all = store.findAll(QuerySpec.none()).collect(Collectors.toList());
        assertThat(all)
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(t2);


        var drs = all.stream().map(TransferProcess::getDataRequest).collect(Collectors.toList());
        assertThat(drs).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsOnly(t2.getDataRequest());
    }


}