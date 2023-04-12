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
import org.eclipse.edc.connector.transfer.spi.types.TransferType;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.hamcrest.Matchers;
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
import java.util.stream.IntStream;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

public abstract class TransferProcessStoreTestBase {
    protected static final String CONNECTOR_NAME = "test-connector";

    public TransferProcessStoreTestBase() {
        System.setProperty("transferprocessstore.supports.operator.like", String.valueOf(supportsLikeOperator()));
        System.setProperty("transferprocessstore.supports.operator.in", String.valueOf(supportsInOperator()));
        System.setProperty("transferprocessstore.supports.collectionQuery", String.valueOf(supportsCollectionQuery()));
        System.setProperty("transferprocessstore.supports.sortorder", String.valueOf(supportsSortOrder()));

    }

    @Test
    void create() {
        var t = TestFunctions.createTransferProcessBuilder("test-id").properties(Map.of("key", "value")).build();
        getTransferProcessStore().save(t);

        var all = getTransferProcessStore().findAll(QuerySpec.none()).collect(Collectors.toList());
        assertThat(all).containsExactly(t);
        assertThat(all.get(0)).usingRecursiveComparison().isEqualTo(t);
        assertThat(all).allSatisfy(tr -> assertThat(tr.getCreatedAt()).isNotEqualTo(0L));
    }

    @Test
    void create_verifyCallbacks() {

        var callbacks = List.of(CallbackAddress.Builder.newInstance().uri("test").events(Set.of("event")).build());

        var t = TestFunctions.createTransferProcessBuilder("test-id").properties(Map.of("key", "value")).callbackAddresses(callbacks).build();
        getTransferProcessStore().save(t);

        var all = getTransferProcessStore().findAll(QuerySpec.none()).collect(Collectors.toList());
        assertThat(all).containsExactly(t);
        assertThat(all.get(0)).usingRecursiveComparison().isEqualTo(t);
        assertThat(all.get(0).getCallbackAddresses()).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsAll(callbacks);
    }

    @Test
    void create_withSameIdExists_shouldReplace() {
        var t = TestFunctions.createTransferProcess("id1", INITIAL);
        getTransferProcessStore().save(t);

        var t2 = TestFunctions.createTransferProcess("id1", PROVISIONING);
        getTransferProcessStore().save(t2);

        assertThat(getTransferProcessStore().findAll(QuerySpec.none())).hasSize(1).containsExactly(t2);
    }

    @Test
    void nextForState() {
        var state = STARTED;
        var all = IntStream.range(0, 10)
                .mapToObj(i -> TestFunctions.createTransferProcess("id" + i, state))
                .peek(getTransferProcessStore()::save)
                .collect(Collectors.toList());

        assertThat(getTransferProcessStore().nextForState(state.code(), 5))
                .hasSize(5)
                .extracting(TransferProcess::getId)
                .isSubsetOf(all.stream().map(TransferProcess::getId).collect(Collectors.toList()))
                .allMatch(id -> isLockedBy(id, CONNECTOR_NAME));
    }

    @Test
    void nextForState_shouldOnlyReturnFreeItems() {
        var state = STARTED;
        var all = IntStream.range(0, 10)
                .mapToObj(i -> TestFunctions.createTransferProcess("id" + i, state))
                .peek(getTransferProcessStore()::save)
                .collect(Collectors.toList());

        // lease a few
        var leasedTp = all.stream().skip(5).peek(tp -> lockEntity(tp.getId(), CONNECTOR_NAME)).collect(Collectors.toList());

        // should not contain leased TPs
        assertThat(getTransferProcessStore().nextForState(state.code(), 10))
                .hasSize(5)
                .isSubsetOf(all)
                .doesNotContainAnyElementsOf(leasedTp);
    }

    @Test
    void nextForState_noFreeItem_shouldReturnEmpty() {
        var state = STARTED;
        IntStream.range(0, 3)
                .mapToObj(i -> TestFunctions.createTransferProcess("id" + i, state))
                .forEach(getTransferProcessStore()::save);

        // first time works
        assertThat(getTransferProcessStore().nextForState(state.code(), 10)).hasSize(3);
        // second time returns empty list
        assertThat(getTransferProcessStore().nextForState(state.code(), 10)).isEmpty();
    }

    @Test
    void nextForState_noneInDesiredState() {
        IntStream.range(0, 3)
                .mapToObj(i -> TestFunctions.createTransferProcess("id" + i, STARTED))
                .forEach(getTransferProcessStore()::save);

        var nextForState = getTransferProcessStore().nextForState(TERMINATED.code(), 10);

        assertThat(nextForState).isEmpty();
    }

    @Test
    void nextForState_batchSizeLimits() {
        var state = STARTED;
        IntStream.range(0, 10)
                .mapToObj(i -> TestFunctions.createTransferProcess("id" + i, state))
                .forEach(getTransferProcessStore()::save);

        // first time works
        assertThat(getTransferProcessStore().nextForState(state.code(), 3)).hasSize(3);
    }

    @Test
    void nextForState_verifyTemporalOrdering() {
        var state = STARTED;
        IntStream.range(0, 10)
                .mapToObj(i -> TestFunctions.createTransferProcess(String.valueOf(i), state))
                .peek(t -> {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                        // noop
                    }
                    t.updateStateTimestamp();
                })
                .forEach(getTransferProcessStore()::save);

        assertThat(getTransferProcessStore().nextForState(state.code(), 20))
                .extracting(TransferProcess::getId)
                .map(Integer::parseInt)
                .isSortedAccordingTo(Integer::compareTo);
    }

    @Test
    void nextForState_verifyMostRecentlyUpdatedIsLast() throws InterruptedException {
        var state = STARTED;
        var all = IntStream.range(0, 10)
                .mapToObj(i -> TestFunctions.createTransferProcess("id" + i, state))
                .peek(getTransferProcessStore()::save)
                .collect(Collectors.toList());

        Thread.sleep(100);

        var fourth = all.get(3);
        fourth.updateStateTimestamp();
        getTransferProcessStore().save(fourth);

        var next = getTransferProcessStore().nextForState(STARTED.code(), 20);
        assertThat(next.indexOf(fourth)).isEqualTo(9);
    }

    @Test
    @DisplayName("Verifies that calling nextForState locks the TP for any subsequent calls")
    void nextForState_locksEntity() {
        var t = TestFunctions.createTransferProcess("id1", INITIAL);
        getTransferProcessStore().save(t);

        getTransferProcessStore().nextForState(INITIAL.code(), 100);

        assertThat(isLockedBy(t.getId(), CONNECTOR_NAME)).isTrue();
    }

    @Test
    void nextForState_expiredLease() {
        var t = TestFunctions.createTransferProcess("id1", INITIAL);
        getTransferProcessStore().save(t);

        lockEntity(t.getId(), CONNECTOR_NAME, Duration.ofMillis(100));

        Awaitility.await().atLeast(Duration.ofMillis(100))
                .atMost(Duration.ofMillis(500))
                .until(() -> getTransferProcessStore().nextForState(INITIAL.code(), 10), Matchers.hasSize(1));
    }

    @Test
    void find() {
        var t = TestFunctions.createTransferProcess("id1");
        getTransferProcessStore().save(t);

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

        var dr = TestFunctions.createDataRequest(pid);
        var t = TestFunctions.createTransferProcess(tid, dr);

        getTransferProcessStore().save(t);

        assertThat(getTransferProcessStore().processIdForDataRequestId(dr.getId())).isEqualTo("transfer-id1");
    }

    @Test
    void processIdForTransferId_notExist() {
        assertThat(getTransferProcessStore().processIdForDataRequestId("not-exist")).isNull();
    }

    @Test
    void update_shouldPersistDataRequest() {
        var t1 = TestFunctions.createTransferProcess("id1", STARTED);
        getTransferProcessStore().save(t1);

        t1.getDataRequest().getProperties().put("newKey", "newValue");
        getTransferProcessStore().save(t1);

        var all = getTransferProcessStore().findAll(QuerySpec.none()).collect(Collectors.toList());
        assertThat(all)
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(t1);

        assertThat(all.get(0).getDataRequest().getProperties()).containsEntry("newKey", "newValue");
    }

    @Test
    void update_exists_shouldUpdate() {
        var t1 = TestFunctions.createTransferProcess("id1", STARTED);
        getTransferProcessStore().save(t1);

        t1.transitionCompleted(); //modify
        getTransferProcessStore().save(t1);

        assertThat(getTransferProcessStore().findAll(QuerySpec.none()))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(t1);
    }

    @Test
    void update_notExist_shouldCreate() {
        var t1 = TestFunctions.createTransferProcess("id1", STARTED);

        t1.transitionCompleted(); //modify
        getTransferProcessStore().save(t1);

        var result = getTransferProcessStore().findAll(QuerySpec.none()).collect(Collectors.toList());
        assertThat(result)
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(t1);
    }

    @Test
    @DisplayName("Verify that the lease on a TP is cleared by an update")
    void update_shouldBreakLease() {
        var t1 = TestFunctions.createTransferProcess("id1");
        getTransferProcessStore().save(t1);
        // acquire lease
        lockEntity(t1.getId(), CONNECTOR_NAME);

        t1.transitionProvisioning(ResourceManifest.Builder.newInstance().build()); //modify
        getTransferProcessStore().save(t1);

        // lease should be broken
        assertThat(getTransferProcessStore().nextForState(PROVISIONING.code(), 10)).usingRecursiveFieldByFieldElementComparator().containsExactly(t1);
    }

    @Test
    void update_leasedByOther_shouldThrowException() {
        var tpId = "id1";
        var t1 = TestFunctions.createTransferProcess(tpId);
        getTransferProcessStore().save(t1);
        lockEntity(tpId, "someone");

        t1.transitionProvisioning(ResourceManifest.Builder.newInstance().build()); //modify

        // leased by someone else -> throw exception
        assertThatThrownBy(() -> getTransferProcessStore().save(t1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void delete() {
        var t1 = TestFunctions.createTransferProcess("id1");
        getTransferProcessStore().save(t1);

        getTransferProcessStore().delete("id1");
        assertThat(getTransferProcessStore().findAll(QuerySpec.none())).isEmpty();
    }

    @Test
    void delete_isLeasedBySelf_shouldThrowException() {
        var t1 = TestFunctions.createTransferProcess("id1");
        getTransferProcessStore().save(t1);
        lockEntity(t1.getId(), CONNECTOR_NAME);


        assertThatThrownBy(() -> getTransferProcessStore().delete("id1")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void delete_isLeasedByOther_shouldThrowException() {
        var t1 = TestFunctions.createTransferProcess("id1");
        getTransferProcessStore().save(t1);

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
        var all = IntStream.range(0, 10)
                .mapToObj(i -> TestFunctions.createTransferProcess("id" + i))
                .peek(getTransferProcessStore()::save)
                .collect(Collectors.toList());

        assertThat(getTransferProcessStore().findAll(QuerySpec.none())).containsExactlyInAnyOrderElementsOf(all);
    }

    @Test
    void findAll_verifyPaging() {
        IntStream.range(0, 10)
                .mapToObj(i -> TestFunctions.createTransferProcess(String.valueOf(i)))
                .forEach(getTransferProcessStore()::save);

        var qs = QuerySpec.Builder.newInstance().limit(5).offset(3).build();
        assertThat(getTransferProcessStore().findAll(qs)).hasSize(5)
                .extracting(TransferProcess::getId)
                .map(Integer::parseInt)
                .allMatch(id -> id >= 3 && id < 8);
    }

    @Test
    void findAll_verifyPaging_pageSizeLargerThanCollection() {

        IntStream.range(0, 10)
                .mapToObj(i -> TestFunctions.createTransferProcess(String.valueOf(i)))
                .forEach(getTransferProcessStore()::save);

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
                .mapToObj(i -> TestFunctions.createTransferProcess(String.valueOf(i)))
                .forEach(getTransferProcessStore()::save);

        var qs = QuerySpec.Builder.newInstance().limit(10).offset(12).build();
        assertThat(getTransferProcessStore().findAll(qs)).isEmpty();

    }

    @Test
    void update_dataRequestWithNewId_replacesOld() {
        var bldr = TestFunctions.createTransferProcessBuilder("id1").state(STARTED.code());
        var t1 = bldr.build();
        getTransferProcessStore().save(t1);

        var t2 = bldr
                .dataRequest(TestFunctions.createDataRequestBuilder()
                        .id("new-dr-id")
                        .assetId("new-asset")
                        .contractId("new-contract")
                        .protocol("test-protocol")
                        .connectorId("new-connector")
                        .build())
                .build();
        getTransferProcessStore().save(t2);

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
        getTransferProcessStore().save(tp);
        getTransferProcessStore().save(createTransferProcess("testprocess2"));

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
        getTransferProcessStore().save(tp);
        getTransferProcessStore().save(createTransferProcess("testprocess2"));

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
        getTransferProcessStore().save(tp);
        getTransferProcessStore().save(createTransferProcess("testprocess2"));

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
        getTransferProcessStore().save(tp);
        getTransferProcessStore().save(createTransferProcess("testprocess2"));

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
        getTransferProcessStore().save(tp);
        getTransferProcessStore().save(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("dataRequest.id", "=", da.getId())))
                .build();

        var result = getTransferProcessStore().findAll(query);

        assertThat(result).usingRecursiveFieldByFieldElementComparatorIgnoringFields("deprovisionedResources").containsOnly(tp);
    }

    @Test
    @EnabledIfSystemProperty(named = "transferprocessstore.supports.operator.like", matches = "true", disabledReason = "This test only runs if the LIKE operator is supported")
    void find_queryByDataRequestProperty_transferType() {
        var da = createDataRequestBuilder().transferType(TransferType.Builder.transferType().contentType("test/contenttype").build())
                .build();
        var tp = createTransferProcessBuilder("testprocess1")
                .dataRequest(da)
                .build();
        getTransferProcessStore().save(tp);
        getTransferProcessStore().save(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("dataRequest.transferType.contentType", "like", "%/contenttype")))
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
        getTransferProcessStore().save(tp);
        getTransferProcessStore().save(createTransferProcess("testprocess2"));

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
        getTransferProcessStore().save(tp);
        getTransferProcessStore().save(createTransferProcess("testprocess2"));

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
        getTransferProcessStore().save(tp);
        getTransferProcessStore().save(createTransferProcess("testprocess2"));

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
        getTransferProcessStore().save(tp);
        getTransferProcessStore().save(createTransferProcess("testprocess2"));

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
        getTransferProcessStore().save(tp);
        getTransferProcessStore().save(createTransferProcess("testprocess2"));


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

        getTransferProcessStore().save(process1);
        getTransferProcessStore().save(process2);

        var query = QuerySpec.Builder.newInstance()
                .filter("deprovisionedResources.inProcess=true")
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

        getTransferProcessStore().save(process1);
        getTransferProcessStore().save(process2);

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

        getTransferProcessStore().save(process1);
        getTransferProcessStore().save(process2);

        var query = QuerySpec.Builder.newInstance()
                .filter("deprovisionedResources.inProcess=false")
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
        getTransferProcessStore().save(process1);

        var query = QuerySpec.Builder.newInstance()
                .filter("deprovisionedResources.foobar=barbaz")
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
        getTransferProcessStore().save(process1);

        var query = QuerySpec.Builder.newInstance()
                .filter("deprovisionedResources.errorMessage=notexist")
                .build();

        var result = getTransferProcessStore().findAll(query).collect(Collectors.toList());

        assertThat(result).isEmpty();
    }

    @Test
    void verifyCreateUpdateDelete() {
        var id = UUID.randomUUID().toString();
        var transferProcess = createTransferProcess(id, createDataRequestBuilder().id("clientid").build());
        getTransferProcessStore().save(transferProcess);

        TransferProcess found = getTransferProcessStore().find(id);

        assertNotNull(found);
        assertNotSame(found, transferProcess); // enforce by-value

        assertNotNull(getTransferProcessStore().processIdForDataRequestId("clientid"));

        assertEquals(INITIAL.code(), found.getState());

        transferProcess.transitionProvisioning(ResourceManifest.Builder.newInstance().build());

        getTransferProcessStore().save(transferProcess);

        found = getTransferProcessStore().find(id);
        assertNotNull(found);
        assertEquals(PROVISIONING.code(), found.getState());

        getTransferProcessStore().delete(id);
        assertNull(getTransferProcessStore().find(id));
        assertNull(getTransferProcessStore().processIdForDataRequestId("clientid"));

    }

    @Test
    void verifyNext() throws InterruptedException {
        var transferProcess1 = initialTransferProcess(UUID.randomUUID().toString(), "req1");
        getTransferProcessStore().save(transferProcess1);
        var transferProcess2 = initialTransferProcess(UUID.randomUUID().toString(), "req2");
        getTransferProcessStore().save(transferProcess2);

        transferProcess2.transitionProvisioning(ResourceManifest.Builder.newInstance().build());
        getTransferProcessStore().save(transferProcess2);
        Thread.sleep(1);
        transferProcess1.transitionProvisioning(ResourceManifest.Builder.newInstance().build());
        getTransferProcessStore().save(transferProcess1);

        assertThat(getTransferProcessStore().nextForState(INITIAL.code(), 1)).isEmpty();

        var found = getTransferProcessStore().nextForState(PROVISIONING.code(), 1);
        assertThat(found).hasSize(1).first().matches(it -> it.equals(transferProcess2));

        found = getTransferProcessStore().nextForState(PROVISIONING.code(), 3);
        assertThat(found).hasSize(1).first().matches(it -> it.equals(transferProcess1));
    }

    @Test
    void nextForState_shouldLeaseEntityUntilUpdate() {
        var initialTransferProcess = initialTransferProcess();
        getTransferProcessStore().save(initialTransferProcess);

        var firstQueryResult = getTransferProcessStore().nextForState(INITIAL.code(), 1);
        assertThat(firstQueryResult).hasSize(1);

        var secondQueryResult = getTransferProcessStore().nextForState(INITIAL.code(), 1);
        assertThat(secondQueryResult).hasSize(0);

        var retrieved = firstQueryResult.get(0);
        getTransferProcessStore().save(retrieved);

        var thirdQueryResult = getTransferProcessStore().nextForState(INITIAL.code(), 1);
        assertThat(thirdQueryResult).hasSize(1);
    }

    @Test
    void verifyMutlipleRequets() {
        String id1 = UUID.randomUUID().toString();
        TransferProcess transferProcess1 = createTransferProcess(id1, createDataRequestBuilder().id("clientid1").build());
        getTransferProcessStore().save(transferProcess1);

        String id2 = UUID.randomUUID().toString();
        TransferProcess transferProcess2 = createTransferProcess(id2, createDataRequestBuilder().id("clientid2").build());
        getTransferProcessStore().save(transferProcess2);


        TransferProcess found1 = getTransferProcessStore().find(id1);
        assertNotNull(found1);

        TransferProcess found2 = getTransferProcessStore().find(id2);
        assertNotNull(found2);

        var found = getTransferProcessStore().nextForState(INITIAL.code(), 3);
        org.junit.jupiter.api.Assertions.assertEquals(2, found.size());
    }

    @Test
    void verifyOrderingByTimestamp() {
        for (int i = 0; i < 100; i++) {
            var process = createTransferProcess("test-process-" + i);
            getTransferProcessStore().save(process);
        }

        var processes = getTransferProcessStore().nextForState(INITIAL.code(), 50);

        assertThat(processes).hasSize(50).allMatch(p -> p.getStateTimestamp() > 0);
    }

    @Test
    void verifyNextForState_avoidsStarvation() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            var process = createTransferProcess("test-process-" + i);
            getTransferProcessStore().save(process);
        }

        var list1 = getTransferProcessStore().nextForState(INITIAL.code(), 5);
        Thread.sleep(50); //simulate a short delay to generate different timestamps
        list1.forEach(tp -> {
            tp.updateStateTimestamp();
            getTransferProcessStore().save(tp);
        });
        var list2 = getTransferProcessStore().nextForState(INITIAL.code(), 5);
        assertThat(list1).isNotEqualTo(list2).doesNotContainAnyElementsOf(list2);
    }

    @Test
    void findAll_verifyFiltering() {
        IntStream.range(0, 10).forEach(i -> getTransferProcessStore().save(createTransferProcess("test-neg-" + i)));
        assertThat(getTransferProcessStore().findAll(QuerySpec.Builder.newInstance().equalsAsContains(false).filter("id=test-neg-3").build())).extracting(TransferProcess::getId).containsOnly("test-neg-3");
    }

    @Test
    void findAll_verifyFiltering_invalidFilterExpression() {
        IntStream.range(0, 10).forEach(i -> getTransferProcessStore().save(createTransferProcess("test-neg-" + i)));
        assertThatThrownBy(() -> getTransferProcessStore().findAll(QuerySpec.Builder.newInstance().filter("something foobar other").build())).isInstanceOfAny(IllegalArgumentException.class);
    }

    @Test
    @EnabledIfSystemProperty(named = "transferprocessstore.supports.sortorder", matches = "true", disabledReason = "This test only runs if sorting is supported")
    void findAll_verifySorting() {
        IntStream.range(0, 10).forEach(i -> getTransferProcessStore().save(createTransferProcess("test-neg-" + i)));

        assertThat(getTransferProcessStore().findAll(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build())).hasSize(10).isSortedAccordingTo(Comparator.comparing(TransferProcess::getId));
        assertThat(getTransferProcessStore().findAll(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build())).hasSize(10).isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));
    }

    @Test
    @EnabledIfSystemProperty(named = "transferprocessstore.supports.sortorder", matches = "true", disabledReason = "This test only runs if sorting is supported")
    protected void findAll_verifySorting_invalidProperty() {
        IntStream.range(0, 10).forEach(i -> getTransferProcessStore().save(createTransferProcess("test-neg-" + i)));

        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

        assertThat(getTransferProcessStore().findAll(query).collect(Collectors.toList())).isEmpty();
    }

    protected abstract boolean supportsCollectionQuery();

    protected abstract boolean supportsLikeOperator();

    protected abstract boolean supportsInOperator();

    protected abstract boolean supportsSortOrder();

    protected abstract TransferProcessStore getTransferProcessStore();

    protected abstract void lockEntity(String negotiationId, String owner, Duration duration);

    protected void lockEntity(String negotiationId, String owner) {
        lockEntity(negotiationId, owner, Duration.ofSeconds(60));
    }

    protected abstract boolean isLockedBy(String negotiationId, String owner);

}
