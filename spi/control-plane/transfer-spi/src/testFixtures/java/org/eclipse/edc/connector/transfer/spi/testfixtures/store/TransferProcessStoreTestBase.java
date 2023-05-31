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

package org.eclipse.edc.connector.transfer.spi.testfixtures.store;

import org.awaitility.Awaitility;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResourceSet;
import org.eclipse.edc.connector.transfer.spi.types.ResourceManifest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.transfer.spi.testfixtures.store.TestFunctions.createDataAddressBuilder;
import static org.eclipse.edc.connector.transfer.spi.testfixtures.store.TestFunctions.createDataRequest;
import static org.eclipse.edc.connector.transfer.spi.testfixtures.store.TestFunctions.createDataRequestBuilder;
import static org.eclipse.edc.connector.transfer.spi.testfixtures.store.TestFunctions.createTransferProcess;
import static org.eclipse.edc.connector.transfer.spi.testfixtures.store.TestFunctions.createTransferProcessBuilder;
import static org.eclipse.edc.connector.transfer.spi.testfixtures.store.TestFunctions.initialTransferProcess;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.PROVISIONING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

public abstract class TransferProcessStoreTestBase {
    protected static final String CONNECTOR_NAME = "test-connector";

    public TransferProcessStoreTestBase() {
        System.setProperty("transferprocessstore.supports.operator.like", String.valueOf(supportsLikeOperator()));
        System.setProperty("transferprocessstore.supports.collectionQuery", String.valueOf(supportsCollectionQuery()));
    }

    @Test
    void create() {
        var t = createTransferProcessBuilder("test-id").privateProperties(Map.of("key", "value")).build();
        getTransferProcessStore().updateOrCreate(t);

        var all = getTransferProcessStore().findAll(QuerySpec.none()).collect(Collectors.toList());
        assertThat(all).containsExactly(t);
        assertThat(all.get(0)).usingRecursiveComparison().isEqualTo(t);
        assertThat(all).allSatisfy(tr -> assertThat(tr.getCreatedAt()).isNotEqualTo(0L));
    }

    @Test
    void create_verifyCallbacks() {

        var callbacks = List.of(CallbackAddress.Builder.newInstance().uri("test").events(Set.of("event")).build());

        var t = createTransferProcessBuilder("test-id").privateProperties(Map.of("key", "value")).callbackAddresses(callbacks).build();
        getTransferProcessStore().updateOrCreate(t);

        var all = getTransferProcessStore().findAll(QuerySpec.none()).collect(Collectors.toList());
        assertThat(all).containsExactly(t);
        assertThat(all.get(0)).usingRecursiveComparison().isEqualTo(t);
        assertThat(all.get(0).getCallbackAddresses()).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsAll(callbacks);
    }

    @Test
    void create_withSameIdExists_shouldReplace() {
        var t = createTransferProcess("id1", INITIAL);
        getTransferProcessStore().updateOrCreate(t);

        var t2 = createTransferProcess("id1", PROVISIONING);
        getTransferProcessStore().updateOrCreate(t2);

        assertThat(getTransferProcessStore().findAll(QuerySpec.none())).hasSize(1).containsExactly(t2);
    }

    @Test
    void nextNotLeased() {
        var state = STARTED;
        var all = range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .peek(getTransferProcessStore()::updateOrCreate)
                .toList();

        assertThat(getTransferProcessStore().nextNotLeased(5, hasState(state.code())))
                .hasSize(5)
                .extracting(TransferProcess::getId)
                .isSubsetOf(all.stream().map(TransferProcess::getId).collect(Collectors.toList()))
                .allMatch(id -> isLockedBy(id, CONNECTOR_NAME));
    }

    @Test
    void nextNotLeased_shouldOnlyReturnFreeItems() {
        var state = STARTED;
        var all = range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .peek(getTransferProcessStore()::updateOrCreate)
                .collect(Collectors.toList());

        // lease a few
        var leasedTp = all.stream().skip(5).peek(tp -> lockEntity(tp.getId(), CONNECTOR_NAME)).toList();

        // should not contain leased TPs
        assertThat(getTransferProcessStore().nextNotLeased(10, hasState(state.code())))
                .hasSize(5)
                .isSubsetOf(all)
                .doesNotContainAnyElementsOf(leasedTp);
    }

    @Test
    void nextNotLeased_noFreeItem_shouldReturnEmpty() {
        var state = STARTED;
        range(0, 3)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .forEach(getTransferProcessStore()::updateOrCreate);

        // first time works
        assertThat(getTransferProcessStore().nextNotLeased(10, hasState(state.code()))).hasSize(3);
        // second time returns empty list
        assertThat(getTransferProcessStore().nextNotLeased(10, hasState(state.code()))).isEmpty();
    }

    @Test
    void nextNotLeased_noneInDesiredState() {
        range(0, 3)
                .mapToObj(i -> createTransferProcess("id" + i, STARTED))
                .forEach(getTransferProcessStore()::updateOrCreate);

        var nextNotLeased = getTransferProcessStore().nextNotLeased(10, hasState(TERMINATED.code()));

        assertThat(nextNotLeased).isEmpty();
    }

    @Test
    void nextNotLeased_batchSizeLimits() {
        var state = STARTED;
        range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i, state))
                .forEach(getTransferProcessStore()::updateOrCreate);

        // first time works
        var result = getTransferProcessStore().nextNotLeased(3, hasState(state.code()));
        assertThat(result).hasSize(3);
    }

    @Test
    void nextNotLeased_verifyTemporalOrdering() {
        var state = STARTED;
        range(0, 10)
                .mapToObj(i -> createTransferProcess(String.valueOf(i), state))
                .peek(this::delayByTenMillis)
                .forEach(getTransferProcessStore()::updateOrCreate);

        assertThat(getTransferProcessStore().nextNotLeased(20, hasState(state.code())))
                .extracting(TransferProcess::getId)
                .map(Integer::parseInt)
                .isSortedAccordingTo(Integer::compareTo);
    }

    @Test
    void nextNotLeased_verifyMostRecentlyUpdatedIsLast() throws InterruptedException {
        var all = range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i, STARTED))
                .peek(getTransferProcessStore()::updateOrCreate)
                .toList();

        Thread.sleep(100);

        var fourth = all.get(3);
        fourth.updateStateTimestamp();
        getTransferProcessStore().updateOrCreate(fourth);

        var next = getTransferProcessStore().nextNotLeased(20, hasState(STARTED.code()));
        assertThat(next.indexOf(fourth)).isEqualTo(9);
    }

    @Test
    @DisplayName("Verifies that calling nextNotLeased locks the TP for any subsequent calls")
    void nextNotLeased_locksEntity() {
        var t = createTransferProcess("id1", INITIAL);
        getTransferProcessStore().updateOrCreate(t);

        getTransferProcessStore().nextNotLeased(100, hasState(INITIAL.code()));

        assertThat(isLockedBy(t.getId(), CONNECTOR_NAME)).isTrue();
    }

    @Test
    void nextNotLeased_expiredLease() {
        var t = createTransferProcess("id1", INITIAL);
        getTransferProcessStore().updateOrCreate(t);

        lockEntity(t.getId(), CONNECTOR_NAME, Duration.ofMillis(100));

        Awaitility.await().atLeast(Duration.ofMillis(100))
                .atMost(Duration.ofMillis(500))
                .until(() -> getTransferProcessStore().nextNotLeased(10, hasState(INITIAL.code())), hasSize(1));
    }

    @Test
    void nextNotLeased_shouldLeaseEntityUntilUpdate() {
        var initialTransferProcess = initialTransferProcess();
        getTransferProcessStore().updateOrCreate(initialTransferProcess);

        var firstQueryResult = getTransferProcessStore().nextNotLeased(1, hasState(INITIAL.code()));
        assertThat(firstQueryResult).hasSize(1);

        var secondQueryResult = getTransferProcessStore().nextNotLeased(1, hasState(INITIAL.code()));
        assertThat(secondQueryResult).hasSize(0);

        var retrieved = firstQueryResult.get(0);
        getTransferProcessStore().updateOrCreate(retrieved);

        var thirdQueryResult = getTransferProcessStore().nextNotLeased(1, hasState(INITIAL.code()));
        assertThat(thirdQueryResult).hasSize(1);
    }

    @Test
    void nextNotLeased_avoidsStarvation() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            var process = createTransferProcess("test-process-" + i);
            getTransferProcessStore().updateOrCreate(process);
        }

        var list1 = getTransferProcessStore().nextNotLeased(5, hasState(INITIAL.code()));
        Thread.sleep(50); //simulate a short delay to generate different timestamps
        list1.forEach(tp -> {
            tp.updateStateTimestamp();
            getTransferProcessStore().updateOrCreate(tp);
        });
        var list2 = getTransferProcessStore().nextNotLeased(5, hasState(INITIAL.code()));
        assertThat(list1).isNotEqualTo(list2).doesNotContainAnyElementsOf(list2);
    }

    @Test
    void findById() {
        var t = createTransferProcess("id1");
        getTransferProcessStore().updateOrCreate(t);

        var result = getTransferProcessStore().findById("id1");

        assertThat(result).usingRecursiveComparison().isEqualTo(t);
    }

    @Test
    void findById_notExist() {
        var result = getTransferProcessStore().findById("not-exist");

        assertThat(result).isNull();
    }

    @Test
    void findForCorrelationId() {
        var dataRequest = createDataRequestBuilder().id("correlationId").build();
        var transferProcess = createTransferProcessBuilder("id1").dataRequest(dataRequest).build();
        getTransferProcessStore().updateOrCreate(transferProcess);

        var res = getTransferProcessStore().findForCorrelationId("correlationId");

        assertThat(res).usingRecursiveComparison().isEqualTo(transferProcess);
    }

    @Test
    void findForCorrelationId_notExist() {
        assertThat(getTransferProcessStore().findForCorrelationId("not-exist")).isNull();
    }

    @Test
    void update_shouldPersistDataRequest() {
        var t1 = createTransferProcess("id1", STARTED);
        getTransferProcessStore().updateOrCreate(t1);

        t1.getDataRequest().getProperties().put("newKey", "newValue");
        getTransferProcessStore().updateOrCreate(t1);

        var all = getTransferProcessStore().findAll(QuerySpec.none()).collect(Collectors.toList());
        assertThat(all)
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(t1);

        assertThat(all.get(0).getDataRequest().getProperties()).containsEntry("newKey", "newValue");
    }

    @Test
    void update_exists_shouldUpdate() {
        var t1 = createTransferProcess("id1", STARTED);
        getTransferProcessStore().updateOrCreate(t1);

        t1.transitionCompleted(); //modify
        getTransferProcessStore().updateOrCreate(t1);

        assertThat(getTransferProcessStore().findAll(QuerySpec.none()))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(t1);
    }

    @Test
    void update_notExist_shouldCreate() {
        var t1 = createTransferProcess("id1", STARTED);

        t1.transitionCompleted(); //modify
        getTransferProcessStore().updateOrCreate(t1);

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
        getTransferProcessStore().updateOrCreate(t1);
        // acquire lease
        lockEntity(t1.getId(), CONNECTOR_NAME);

        t1.transitionProvisioning(ResourceManifest.Builder.newInstance().build()); //modify
        getTransferProcessStore().updateOrCreate(t1);

        // lease should be broken
        var notLeased = getTransferProcessStore().nextNotLeased(10, hasState(PROVISIONING.code()));

        assertThat(notLeased).usingRecursiveFieldByFieldElementComparator().containsExactly(t1);
    }

    @Test
    void update_leasedByOther_shouldThrowException() {
        var tpId = "id1";
        var t1 = createTransferProcess(tpId);
        getTransferProcessStore().updateOrCreate(t1);
        lockEntity(tpId, "someone");

        t1.transitionProvisioning(ResourceManifest.Builder.newInstance().build()); //modify

        // leased by someone else -> throw exception
        assertThatThrownBy(() -> getTransferProcessStore().updateOrCreate(t1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void delete() {
        var t1 = createTransferProcess("id1");
        getTransferProcessStore().updateOrCreate(t1);

        getTransferProcessStore().delete("id1");
        assertThat(getTransferProcessStore().findAll(QuerySpec.none())).isEmpty();
    }

    @Test
    void delete_isLeasedBySelf_shouldThrowException() {
        var t1 = createTransferProcess("id1");
        getTransferProcessStore().updateOrCreate(t1);
        lockEntity(t1.getId(), CONNECTOR_NAME);


        assertThatThrownBy(() -> getTransferProcessStore().delete("id1")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void delete_isLeasedByOther_shouldThrowException() {
        var t1 = createTransferProcess("id1");
        getTransferProcessStore().updateOrCreate(t1);

        lockEntity(t1.getId(), "someone-else");

        assertThatThrownBy(() -> getTransferProcessStore().delete("id1")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void delete_notExist() {
        getTransferProcessStore().delete("not-exist");
        //no exception should be raised
    }

    @Test
    void findAll_noQuerySpec() {
        var all = range(0, 10)
                .mapToObj(i -> createTransferProcess("id" + i))
                .peek(getTransferProcessStore()::updateOrCreate)
                .collect(Collectors.toList());

        assertThat(getTransferProcessStore().findAll(QuerySpec.none())).containsExactlyInAnyOrderElementsOf(all);
    }

    @Test
    void findAll_verifyPaging() {
        range(0, 10)
                .mapToObj(i -> createTransferProcess(String.valueOf(i)))
                .forEach(getTransferProcessStore()::updateOrCreate);

        var qs = QuerySpec.Builder.newInstance().limit(5).offset(3).build();
        assertThat(getTransferProcessStore().findAll(qs)).hasSize(5)
                .extracting(TransferProcess::getId)
                .map(Integer::parseInt)
                .allMatch(id -> id >= 3 && id < 8);
    }

    @Test
    void findAll_verifyPaging_pageSizeLargerThanCollection() {

        range(0, 10)
                .mapToObj(i -> createTransferProcess(String.valueOf(i)))
                .forEach(getTransferProcessStore()::updateOrCreate);

        var qs = QuerySpec.Builder.newInstance().limit(20).offset(3).build();
        assertThat(getTransferProcessStore().findAll(qs))
                .hasSize(7)
                .extracting(TransferProcess::getId)
                .map(Integer::parseInt)
                .allMatch(id -> id >= 3 && id < 10);
    }

    @Test
    void findAll_verifyPaging_pageSizeOutsideCollection() {

        range(0, 10)
                .mapToObj(i -> createTransferProcess(String.valueOf(i)))
                .forEach(getTransferProcessStore()::updateOrCreate);

        var qs = QuerySpec.Builder.newInstance().limit(10).offset(12).build();
        assertThat(getTransferProcessStore().findAll(qs)).isEmpty();

    }

    @Test
    void update_dataRequestWithNewId_replacesOld() {
        var bldr = createTransferProcessBuilder("id1").state(STARTED.code());
        var t1 = bldr.build();
        getTransferProcessStore().updateOrCreate(t1);

        var t2 = bldr
                .dataRequest(TestFunctions.createDataRequestBuilder()
                        .id("new-dr-id")
                        .assetId("new-asset")
                        .contractId("new-contract")
                        .protocol("test-protocol")
                        .connectorId("new-connector")
                        .build())
                .build();
        getTransferProcessStore().updateOrCreate(t2);

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

    @Test
    void find_queryByDataAddressProperty() {
        var da = createDataAddressBuilder("test-type")
                .property("key", "value")
                .build();
        var tp = createTransferProcessBuilder("testprocess1")
                .contentDataAddress(da)
                .build();
        getTransferProcessStore().updateOrCreate(tp);
        getTransferProcessStore().updateOrCreate(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("contentDataAddress.properties.key", "=", "value")))
                .build();

        assertThat(getTransferProcessStore().findAll(query))
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(tp);

    }

    @Test
    void find_queryByDataAddress_propNotExist() {
        var da = createDataAddressBuilder("test-type")
                .property("key", "value")
                .build();
        var tp = createTransferProcessBuilder("testprocess1")
                .contentDataAddress(da)
                .build();
        getTransferProcessStore().updateOrCreate(tp);
        getTransferProcessStore().updateOrCreate(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("contentDataAddress.properties.notexist", "=", "value")))
                .build();

        assertThat(getTransferProcessStore().findAll(query)).isEmpty();

    }

    @Test
    void find_queryByDataAddress_invalidKey_valueNotExist() {
        var da = createDataAddressBuilder("test-type")
                .property("key", "value")
                .build();
        var tp = createTransferProcessBuilder("testprocess1")
                .contentDataAddress(da)
                .build();
        getTransferProcessStore().updateOrCreate(tp);
        getTransferProcessStore().updateOrCreate(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("contentDataAddress.properties.key", "=", "notexist")))
                .build();

        assertThat(getTransferProcessStore().findAll(query)).isEmpty();
    }

    @Test
    void find_queryByDataRequestProperty_processId() {
        var da = createDataRequest();
        var tp = createTransferProcessBuilder("testprocess1")
                .dataRequest(da)
                .build();
        getTransferProcessStore().updateOrCreate(tp);
        getTransferProcessStore().updateOrCreate(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("dataRequest.processId", "=", "testprocess1")))
                .build();

        var result = getTransferProcessStore().findAll(query);

        assertThat(result).usingRecursiveFieldByFieldElementComparatorIgnoringFields("deprovisionedResources").containsOnly(tp);
    }

    @Test
    void find_queryByDataRequestProperty_id() {
        var da = createDataRequest();
        var tp = createTransferProcessBuilder("testprocess1")
                .dataRequest(da)
                .build();
        getTransferProcessStore().updateOrCreate(tp);
        getTransferProcessStore().updateOrCreate(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("dataRequest.id", "=", da.getId())))
                .build();

        var result = getTransferProcessStore().findAll(query);

        assertThat(result).usingRecursiveFieldByFieldElementComparatorIgnoringFields("deprovisionedResources").containsOnly(tp);
    }

    @Test
    @EnabledIfSystemProperty(named = "transferprocessstore.supports.operator.like", matches = "true", disabledReason = "This test only runs if the LIKE operator is supported")
    void find_queryByDataRequestProperty_protocol() {
        var da = createDataRequestBuilder().protocol("%/protocol").build();
        var tp = createTransferProcessBuilder("testprocess1")
                .dataRequest(da)
                .build();
        getTransferProcessStore().updateOrCreate(tp);
        getTransferProcessStore().updateOrCreate(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("dataRequest.protocol", "like", "%/protocol")))
                .build();

        var result = getTransferProcessStore().findAll(query);

        assertThat(result).usingRecursiveFieldByFieldElementComparatorIgnoringFields("deprovisionedResources").containsOnly(tp);
    }

    @Test
    void find_queryByDataRequest_valueNotExist() {
        var da = createDataRequest();
        var tp = createTransferProcessBuilder("testprocess1")
                .dataRequest(da)
                .build();
        getTransferProcessStore().updateOrCreate(tp);
        getTransferProcessStore().updateOrCreate(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("dataRequest.id", "=", "notexist")))
                .build();

        assertThat(getTransferProcessStore().findAll(query)).isEmpty();
    }

    @Test
    @EnabledIfSystemProperty(named = "transferprocessstore.supports.collectionQuery", matches = "true", disabledReason = "This test only runs if querying collection fields is supported")
    void find_queryByResourceManifestProperty() {
        var rm = ResourceManifest.Builder.newInstance()
                .definitions(List.of(TestFunctions.TestResourceDef.Builder.newInstance().id("rd-id").transferProcessId("testprocess1").build())).build();
        var tp = createTransferProcessBuilder("testprocess1")
                .resourceManifest(rm)
                .build();
        getTransferProcessStore().updateOrCreate(tp);
        getTransferProcessStore().updateOrCreate(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("resourceManifest.definitions.id", "=", "rd-id")))
                .build();

        var result = getTransferProcessStore().findAll(query);
        assertThat(result).usingRecursiveFieldByFieldElementComparatorIgnoringFields("deprovisionedResources").containsOnly(tp);
    }

    @Test
    @EnabledIfSystemProperty(named = "transferprocessstore.supports.collectionQuery", matches = "true", disabledReason = "This test only runs if querying collection fields is supported")
    void find_queryByResourceManifest_valueNotExist() {
        var rm = ResourceManifest.Builder.newInstance()
                .definitions(List.of(TestFunctions.TestResourceDef.Builder.newInstance().id("rd-id").transferProcessId("testprocess1").build())).build();
        var tp = createTransferProcessBuilder("testprocess1")
                .resourceManifest(rm)
                .build();
        getTransferProcessStore().updateOrCreate(tp);
        getTransferProcessStore().updateOrCreate(createTransferProcess("testprocess2"));

        // throws exception when an explicit mapping exists
        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("resourceManifest.definitions.id", "=", "someval")))
                .build();
        assertThat(getTransferProcessStore().findAll(query)).isEmpty();
    }

    @Test
    @EnabledIfSystemProperty(named = "transferprocessstore.supports.collectionQuery", matches = "true", disabledReason = "This test only runs if querying collection fields is supported")
    void find_queryByProvisionedResourceSetProperty() {
        var resource = TestFunctions.TestProvisionedResource.Builder.newInstance()
                .resourceDefinitionId("rd-id")
                .transferProcessId("testprocess1")
                .id("pr-id")
                .build();
        var prs = ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(resource))
                .build();
        var tp = createTransferProcessBuilder("testprocess1")
                .provisionedResourceSet(prs)
                .build();
        getTransferProcessStore().updateOrCreate(tp);
        getTransferProcessStore().updateOrCreate(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("provisionedResourceSet.resources.transferProcessId", "=", "testprocess1")))
                .build();

        var result = getTransferProcessStore().findAll(query);
        assertThat(result).usingRecursiveFieldByFieldElementComparatorIgnoringFields("deprovisionedResources").containsOnly(tp);
    }

    @Test
    @EnabledIfSystemProperty(named = "transferprocessstore.supports.collectionQuery", matches = "true", disabledReason = "This test only runs if querying collection fields is supported")
    void find_queryByProvisionedResourceSet_valueNotExist() {
        var resource = TestFunctions.TestProvisionedResource.Builder.newInstance()
                .resourceDefinitionId("rd-id")
                .transferProcessId("testprocess1")
                .id("pr-id")
                .build();
        var prs = ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(resource))
                .build();
        var tp = createTransferProcessBuilder("testprocess1")
                .provisionedResourceSet(prs)
                .build();
        getTransferProcessStore().updateOrCreate(tp);
        getTransferProcessStore().updateOrCreate(createTransferProcess("testprocess2"));


        // returns empty when the invalid value is embedded in JSON
        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("provisionedResourceSet.resources.id", "=", "someval")))
                .build();

        assertThat(getTransferProcessStore().findAll(query)).isEmpty();
    }

    @Test
    @EnabledIfSystemProperty(named = "transferprocessstore.supports.collectionQuery", matches = "true", disabledReason = "This test only runs if querying collection fields is supported")
    void find_queryByDeprovisionedResourcesProperty() {
        var dp1 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid1")
                .inProcess(true)
                .build();
        var dp2 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid2")
                .inProcess(false)
                .build();
        var dp3 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid3")
                .inProcess(false)
                .build();

        var process1 = createTransferProcessBuilder("test-pid1")
                .deprovisionedResources(List.of(dp1, dp2))
                .build();
        var process2 = createTransferProcessBuilder("test-pid2")
                .deprovisionedResources(List.of(dp3))
                .build();

        getTransferProcessStore().updateOrCreate(process1);
        getTransferProcessStore().updateOrCreate(process2);

        var query = QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion("deprovisionedResources.inProcess", "=", "true"))
                .build();

        var result = getTransferProcessStore().findAll(query).collect(Collectors.toList());

        assertThat(result).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(process1);
    }

    @Test
    @EnabledIfSystemProperty(named = "transferprocessstore.supports.collectionQuery", matches = "true", disabledReason = "This test only runs if querying collection fields is supported")
    void find_queryByDeprovisionedResourcesProperty_multipleCriteria() {
        var dp1 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid1")
                .inProcess(true)
                .build();
        var dp2 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid2")
                .inProcess(false)
                .build();
        var dp3 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid3")
                .inProcess(false)
                .build();

        var process1 = createTransferProcessBuilder("test-pid1")
                .deprovisionedResources(List.of(dp1, dp2))
                .build();
        var process2 = createTransferProcessBuilder("test-pid2")
                .deprovisionedResources(List.of(dp3))
                .build();

        getTransferProcessStore().updateOrCreate(process1);
        getTransferProcessStore().updateOrCreate(process2);

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(
                        new Criterion("deprovisionedResources.inProcess", "=", "false"),
                        new Criterion("id", "=", "test-pid1")
                ))
                .build();

        var result = getTransferProcessStore().findAll(query).collect(Collectors.toList());

        assertThat(result).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(process1);
    }

    @Test
    @EnabledIfSystemProperty(named = "transferprocessstore.supports.collectionQuery", matches = "true", disabledReason = "This test only runs if querying collection fields is supported")
    void find_queryByDeprovisionedResourcesProperty_multipleResults() {
        var dp1 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid1")
                .inProcess(true)
                .build();
        var dp2 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid2")
                .inProcess(false)
                .build();
        var dp3 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid3")
                .inProcess(false)
                .build();

        var process1 = createTransferProcessBuilder("test-pid1")
                .deprovisionedResources(List.of(dp1, dp2))
                .build();
        var process2 = createTransferProcessBuilder("test-pid2")
                .deprovisionedResources(List.of(dp3))
                .build();

        getTransferProcessStore().updateOrCreate(process1);
        getTransferProcessStore().updateOrCreate(process2);

        var query = QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion("deprovisionedResources.inProcess", "=", "false"))
                .build();

        var result = getTransferProcessStore().findAll(query).collect(Collectors.toList());

        assertThat(result).hasSize(2)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(process1, process2);
    }

    @Test
    @EnabledIfSystemProperty(named = "transferprocessstore.supports.collectionQuery", matches = "true", disabledReason = "This test only runs if querying collection fields is supported")
    void find_queryByDeprovisionedResources_propNotExist() {
        var dp1 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid1")
                .inProcess(true)
                .build();
        var dp2 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid2")
                .inProcess(false)
                .build();

        var process1 = createTransferProcessBuilder("test-pid1")
                .deprovisionedResources(List.of(dp1, dp2))
                .build();
        getTransferProcessStore().updateOrCreate(process1);

        var query = QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion("deprovisionedResources.foobar", "=", "barbaz"))
                .build();

        assertThat(getTransferProcessStore().findAll(query)).isEmpty();
    }

    @Test
    @EnabledIfSystemProperty(named = "transferprocessstore.supports.collectionQuery", matches = "true", disabledReason = "This test only runs if querying collection fields is supported")
    void find_queryByDeprovisionedResources_valueNotExist() {
        var dp1 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid1")
                .inProcess(true)
                .errorMessage("not enough resources")
                .build();
        var dp2 = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId("test-rid2")
                .inProcess(false)
                .errorMessage("undefined error")
                .build();

        var process1 = createTransferProcessBuilder("test-pid1")
                .deprovisionedResources(List.of(dp1, dp2))
                .build();
        getTransferProcessStore().updateOrCreate(process1);

        var query = QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion("deprovisionedResources.errorMessage", "=", "notexist"))
                .build();

        var result = getTransferProcessStore().findAll(query).collect(Collectors.toList());

        assertThat(result).isEmpty();
    }

    @Test
    void verifyCreateUpdateDelete() {
        var id = UUID.randomUUID().toString();
        var transferProcess = createTransferProcess(id, createDataRequestBuilder().id("correlationId").build());
        getTransferProcessStore().updateOrCreate(transferProcess);

        TransferProcess found = getTransferProcessStore().findById(id);

        assertNotNull(found);
        assertNotSame(found, transferProcess); // enforce by-value

        assertNotNull(getTransferProcessStore().findForCorrelationId("correlationId"));

        assertEquals(INITIAL.code(), found.getState());

        transferProcess.transitionProvisioning(ResourceManifest.Builder.newInstance().build());

        getTransferProcessStore().updateOrCreate(transferProcess);

        found = getTransferProcessStore().findById(id);
        assertNotNull(found);
        assertEquals(PROVISIONING.code(), found.getState());

        getTransferProcessStore().delete(id);
        assertNull(getTransferProcessStore().findById(id));
        assertNull(getTransferProcessStore().findForCorrelationId("correlationId"));
    }

    @Test
    void findAll_verifyFiltering() {
        range(0, 10).forEach(i -> getTransferProcessStore().updateOrCreate(createTransferProcess("test-neg-" + i)));
        var querySpec = QuerySpec.Builder.newInstance().filter(Criterion.criterion("id", "=", "test-neg-3")).build();

        var result = getTransferProcessStore().findAll(querySpec);

        assertThat(result).extracting(TransferProcess::getId).containsOnly("test-neg-3");
    }

    @Test
    void findAll_verifyFiltering_invalidFilterExpression() {
        range(0, 10).forEach(i -> getTransferProcessStore().updateOrCreate(createTransferProcess("test-neg-" + i)));
        var querySpec = QuerySpec.Builder.newInstance().filter(Criterion.criterion("something", "foobar", "other")).build();

        assertThatThrownBy(() -> getTransferProcessStore().findAll(querySpec)).isInstanceOfAny(IllegalArgumentException.class);
    }

    @Test
    void findAll_verifySorting() {
        range(0, 10).forEach(i -> getTransferProcessStore().updateOrCreate(createTransferProcess("test-neg-" + i)));

        assertThat(getTransferProcessStore().findAll(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build())).hasSize(10).isSortedAccordingTo(Comparator.comparing(TransferProcess::getId));
        assertThat(getTransferProcessStore().findAll(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build())).hasSize(10).isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));
    }

    @Test
    protected void findAll_verifySorting_invalidProperty() {
        range(0, 10).forEach(i -> getTransferProcessStore().updateOrCreate(createTransferProcess("test-neg-" + i)));

        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

        assertThat(getTransferProcessStore().findAll(query).collect(Collectors.toList())).isEmpty();
    }

    private void delayByTenMillis(TransferProcess t) {
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {
            // noop
        }
        t.updateStateTimestamp();
    }


    protected abstract boolean supportsCollectionQuery();

    protected abstract boolean supportsLikeOperator();

    protected abstract TransferProcessStore getTransferProcessStore();

    protected abstract void lockEntity(String negotiationId, String owner, Duration duration);

    protected void lockEntity(String negotiationId, String owner) {
        lockEntity(negotiationId, owner, Duration.ofSeconds(60));
    }

    protected abstract boolean isLockedBy(String negotiationId, String owner);

}
