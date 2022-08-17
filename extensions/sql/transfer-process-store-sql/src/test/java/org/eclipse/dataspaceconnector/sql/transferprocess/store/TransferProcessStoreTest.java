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

    @Test
    void create() {
        var t = createTransferProcess("test-id");
        getTransferProcessStore().create(t);

        var all = getTransferProcessStore().findAll(QuerySpec.none()).collect(Collectors.toList());
        assertThat(all).containsExactly(t);
        assertThat(all.get(0)).usingRecursiveComparison().isEqualTo(t);
        assertThat(all).allSatisfy(tr -> assertThat(tr.getCreatedAt()).isNotEqualTo(0L));
    }

    @Test
    void create_withSameIdExists_shouldReplace() {
        var t = createTransferProcess("id1", TransferProcessStates.UNSAVED);
        getTransferProcessStore().create(t);

        var t2 = createTransferProcess("id1", TransferProcessStates.PROVISIONING);
        getTransferProcessStore().create(t2);

        assertThat(getTransferProcessStore().findAll(QuerySpec.none())).hasSize(1).containsExactly(t2);
    }

    @Test
    void nextForState() {
        var state = TransferProcessStates.IN_PROGRESS;
        var all = IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .peek(getTransferProcessStore()::create)
                .collect(Collectors.toList());

        assertThat(getTransferProcessStore().nextForState(state.code(), 5))
                .hasSize(5)
                .extracting(TransferProcess::getId)
                .isSubsetOf(all.stream().map(TransferProcess::getId).collect(Collectors.toList()))
                .allMatch(id -> getLeaseUtil().isLeased(id, CONNECTOR_NAME));
    }

    @Test
    void nextForState_shouldOnlyReturnFreeItems() {
        var state = TransferProcessStates.IN_PROGRESS;
        var all = IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .peek(getTransferProcessStore()::create)
                .collect(Collectors.toList());

        // lease a few
        var leasedTp = all.stream().skip(5).peek(tp -> getLeaseUtil().leaseEntity(tp.getId(), CONNECTOR_NAME)).collect(Collectors.toList());

        // should not contain leased TPs
        assertThat(getTransferProcessStore().nextForState(state.code(), 10))
                .hasSize(5)
                .isSubsetOf(all)
                .doesNotContainAnyElementsOf(leasedTp);
    }

    @Test
    void nextForState_noFreeItem_shouldReturnEmpty() {
        var state = TransferProcessStates.IN_PROGRESS;
        IntStream.range(0, 3)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .forEach(getTransferProcessStore()::create);

        // first time works
        assertThat(getTransferProcessStore().nextForState(state.code(), 10)).hasSize(3);
        // second time returns empty list
        assertThat(getTransferProcessStore().nextForState(state.code(), 10)).isEmpty();
    }

    @Test
    void nextForState_noneInDesiredState() {
        var state = TransferProcessStates.IN_PROGRESS;
        IntStream.range(0, 3)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .forEach(getTransferProcessStore()::create);

        assertThat(getTransferProcessStore().nextForState(TransferProcessStates.CANCELLED.code(), 10)).isEmpty();
    }

    @Test
    void nextForState_batchSizeLimits() {
        var state = TransferProcessStates.IN_PROGRESS;
        IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .forEach(getTransferProcessStore()::create);

        // first time works
        assertThat(getTransferProcessStore().nextForState(state.code(), 3)).hasSize(3);
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
                .forEach(getTransferProcessStore()::create);

        assertThat(getTransferProcessStore().nextForState(state.code(), 20))
                .extracting(TransferProcess::getId)
                .map(Integer::parseInt)
                .isSortedAccordingTo(Integer::compareTo);
    }

    @Test
    void nextForState_verifyMostRecentlyUpdatedIsLast() throws InterruptedException {
        var state = TransferProcessStates.IN_PROGRESS;
        var all = IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .peek(getTransferProcessStore()::create)
                .collect(Collectors.toList());

        Thread.sleep(100);

        var fourth = all.get(3);
        fourth.updateStateTimestamp();
        getTransferProcessStore().update(fourth);

        var next = getTransferProcessStore().nextForState(TransferProcessStates.IN_PROGRESS.code(), 20);
        assertThat(next.indexOf(fourth)).isEqualTo(9);
    }

    @Test
    @DisplayName("Verifies that calling nextForState locks the TP for any subsequent calls")
    void nextForState_locksEntity() {
        var t = createTransferProcess("id1", TransferProcessStates.UNSAVED);
        getTransferProcessStore().create(t);

        getTransferProcessStore().nextForState(TransferProcessStates.UNSAVED.code(), 100);

        assertThat(getLeaseUtil().isLeased(t.getId(), CONNECTOR_NAME)).isTrue();
    }

    @Test
    void nextForState_expiredLease() {
        var t = createTransferProcess("id1", TransferProcessStates.UNSAVED);
        getTransferProcessStore().create(t);

        getLeaseUtil().leaseEntity(t.getId(), CONNECTOR_NAME, Duration.ofMillis(100));

        await().atLeast(Duration.ofMillis(100))
                .atMost(Duration.ofMillis(500))
                .until(() -> getTransferProcessStore().nextForState(TransferProcessStates.UNSAVED.code(), 10), hasSize(1));
    }

    @Test
    void find() {
        var t = createTransferProcess("id1");
        getTransferProcessStore().create(t);

        var res = getTransferProcessStore().find("id1");

        assertThat(res).usingRecursiveComparison().isEqualTo(t);
    }

    @Test
    void find_notExist() {
        assertThat(getTransferProcessStore().find("not-exist")).isNull();
    }

    @Test
    void processIdForTransferId() {
        var pid = "process-id1";
        var tid = "transfer-id1";

        var dr = createDataRequest(pid);
        var t = createTransferProcess(tid, dr);

        getTransferProcessStore().create(t);

        assertThat(getTransferProcessStore().processIdForTransferId(tid)).isEqualTo("transfer-id1");
    }

    @Test
    void processIdForTransferId_notExist() {
        assertThat(getTransferProcessStore().processIdForTransferId("not-exist")).isNull();
    }

    @Test
    void update_shouldPersistDataRequest() {
        var t1 = createTransferProcess("id1", TransferProcessStates.IN_PROGRESS);
        getTransferProcessStore().create(t1);

        t1.getDataRequest().getProperties().put("newKey", "newValue");
        getTransferProcessStore().update(t1);

        var all = getTransferProcessStore().findAll(QuerySpec.none()).collect(Collectors.toList());
        assertThat(all)
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(t1);

        assertThat(all.get(0).getDataRequest().getProperties()).containsEntry("newKey", "newValue");
    }

    @Test
    void update_exists_shouldUpdate() {
        var t1 = createTransferProcess("id1", TransferProcessStates.IN_PROGRESS);
        getTransferProcessStore().create(t1);

        t1.transitionCompleted(); //modify
        getTransferProcessStore().update(t1);

        assertThat(getTransferProcessStore().findAll(QuerySpec.none()))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(t1);
    }

    @Test
    void update_notExist_shouldCreate() {
        var t1 = createTransferProcess("id1", TransferProcessStates.IN_PROGRESS);

        t1.transitionCompleted(); //modify
        getTransferProcessStore().update(t1);

        var result = getTransferProcessStore().findAll(QuerySpec.none()).collect(Collectors.toList());
        assertThat(result)
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(t1);
    }

    @Test
    @DisplayName("Verify that the lease on a TP is cleared by an update")
    void update_shouldBreakLease() {
        var t1 = createTransferProcess("id1");
        getTransferProcessStore().create(t1);
        // acquire lease
        getLeaseUtil().leaseEntity(t1.getId(), CONNECTOR_NAME);

        t1.transitionInitial(); //modify
        getTransferProcessStore().update(t1);

        // lease should be broken
        assertThat(getTransferProcessStore().nextForState(TransferProcessStates.INITIAL.code(), 10)).usingRecursiveFieldByFieldElementComparator().containsExactly(t1);
    }

    @Test
    void update_leasedByOther_shouldThrowException() {

        var tpId = "id1";
        var t1 = createTransferProcess(tpId);
        getTransferProcessStore().create(t1);
        getLeaseUtil().leaseEntity(tpId, "someone");

        t1.transitionInitial(); //modify

        // leased by someone else -> throw exception
        assertThatThrownBy(() -> getTransferProcessStore().update(t1)).isInstanceOf(IllegalStateException.class);

    }

    @Test
    void delete() {
        var t1 = createTransferProcess("id1");
        getTransferProcessStore().create(t1);

        getTransferProcessStore().delete("id1");
        assertThat(getTransferProcessStore().findAll(QuerySpec.none())).isEmpty();
    }

    @Test
    void delete_isLeasedBySelf_shouldThrowException() {
        var t1 = createTransferProcess("id1");
        getTransferProcessStore().create(t1);
        getLeaseUtil().leaseEntity(t1.getId(), CONNECTOR_NAME);


        assertThatThrownBy(() -> getTransferProcessStore().delete("id1")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void delete_isLeasedByOther_shouldThrowException() {
        var t1 = createTransferProcess("id1");
        getTransferProcessStore().create(t1);

        getLeaseUtil().leaseEntity(t1.getId(), "someone-else");

        assertThatThrownBy(() -> getTransferProcessStore().delete("id1")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void delete_notExist() {
        getTransferProcessStore().delete("not-exist");
        //no exception should be raised
    }

    @Test
    void findAll_noQuerySpec() {
        var all = IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i))
                .peek(getTransferProcessStore()::create)
                .collect(Collectors.toList());

        assertThat(getTransferProcessStore().findAll(QuerySpec.none())).containsExactlyInAnyOrderElementsOf(all);
    }

    @Test
    void findAll_verifyPaging() {
        IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess(String.valueOf(i)))
                .forEach(getTransferProcessStore()::create);

        var qs = QuerySpec.Builder.newInstance().limit(5).offset(3).build();
        assertThat(getTransferProcessStore().findAll(qs)).hasSize(5)
                .extracting(TransferProcess::getId)
                .map(Integer::parseInt)
                .allMatch(id -> id >= 3 && id < 8);
    }

    @Test
    void findAll_verifyPaging_pageSizeLargerThanCollection() {

        IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess(String.valueOf(i)))
                .forEach(getTransferProcessStore()::create);

        var qs = QuerySpec.Builder.newInstance().limit(20).offset(3).build();
        assertThat(getTransferProcessStore().findAll(qs))
                .hasSize(7)
                .extracting(TransferProcess::getId)
                .map(Integer::parseInt)
                .allMatch(id -> id >= 3 && id < 10);
    }

    @Test
    void findAll_verifyPaging_pageSizeOutsideCollection() {

        IntStream.range(0, 10)
                .mapToObj(i -> createTransferProcess(String.valueOf(i)))
                .forEach(getTransferProcessStore()::create);

        var qs = QuerySpec.Builder.newInstance().limit(10).offset(12).build();
        assertThat(getTransferProcessStore().findAll(qs)).isEmpty();

    }

    @Test
    void create_withoutDataRequest_throwsException() {
        var t1 = createTransferProcessBuilder("id1")
                .dataRequest(null)
                .build();
        assertThatIllegalArgumentException().isThrownBy(() -> getTransferProcessStore().create(t1));
    }

    @Test
    void update_dataRequestWithNewId_replacesOld() {
        var bldr = createTransferProcessBuilder("id1").state(TransferProcessStates.IN_PROGRESS.code());
        var t1 = bldr.build();
        getTransferProcessStore().create(t1);

        var t2 = bldr
                .dataRequest(createDataRequestBuilder()
                        .id("new-dr-id")
                        .assetId("new-asset")
                        .contractId("new-contract")
                        .protocol("test-protocol")
                        .connectorId("new-connector")
                        .build())
                .build();
        getTransferProcessStore().update(t2);

        var all = getTransferProcessStore().findAll(QuerySpec.none()).collect(Collectors.toList());
        assertThat(all)
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(t2);


        var drs = all.stream().map(TransferProcess::getDataRequest).collect(Collectors.toList());
        assertThat(drs).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsOnly(t2.getDataRequest());
    }

    protected abstract SqlTransferProcessStore getTransferProcessStore();

    protected abstract LeaseUtil getLeaseUtil();

}