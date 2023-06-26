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

package org.eclipse.edc.connector.defaults.storage.contractnegotiation;


import org.assertj.core.api.Assertions;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.testfixtures.negotiation.store.ContractNegotiationStoreTestBase;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.spi.persistence.Lease;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation.Type.CONSUMER;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.INITIAL;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTING;
import static org.eclipse.edc.connector.defaults.storage.contractnegotiation.TestFunctions.createNegotiationBuilder;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

class InMemoryContractNegotiationStoreTest extends ContractNegotiationStoreTestBase {
    private static final String TEST_ASSET_ID = "test-asset-id";
    private final Map<String, Lease> leases = new HashMap<>();
    private InMemoryContractNegotiationStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryContractNegotiationStore(CONNECTOR_NAME, Clock.systemUTC(), leases);
    }

    @Test
    void verifyCreateDelete() {
        String id = UUID.randomUUID().toString();
        var negotiation = createNegotiationBuilder(id).build();
        negotiation.transitionInitial();

        store.save(negotiation);

        ContractNegotiation found = store.findById(id);

        assertNotNull(found);
        assertNotSame(found, negotiation); // enforce by-value

        store.delete(id);
        assertNull(store.findById(id));
        assertNull(store.findContractAgreement("agreementId"));
    }

    @Test
    void verifyCreateUpdate() {
        String id = UUID.randomUUID().toString();
        var negotiation = TestFunctions.createNegotiation(id);
        negotiation.transitionInitial();

        store.save(negotiation);

        var found = store.findById(id);
        assertNotNull(store.findContractAgreement("agreementId"));

        assertEquals(INITIAL.code(), found.getState());

        negotiation.transitionRequesting();

        store.save(negotiation);
        found = store.findById(id);
        assertNotNull(found);
        assertEquals(REQUESTING.code(), found.getState());

        assertThatThrownBy(() -> store.delete(id)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void verifyNext() throws InterruptedException {
        var negotiation1 = requestingNegotiation();
        store.save(negotiation1);
        var negotiation2 = requestingNegotiation();
        store.save(negotiation2);

        negotiation2.transitionRequested();
        store.save(negotiation2);
        Thread.sleep(1);
        negotiation1.transitionRequested();
        store.save(negotiation1);

        var requestingNegotiations = store.nextNotLeased(1, hasState(REQUESTING.code()));
        assertThat(requestingNegotiations).isEmpty();

        var found = store.nextNotLeased(1, hasState(REQUESTED.code()));
        assertEquals(1, found.size());
        assertEquals(negotiation2, found.get(0));

        found = store.nextNotLeased(3, hasState(REQUESTED.code()));
        assertEquals(1, found.size());
        assertEquals(negotiation1, found.get(0));
    }

    @Test
    void nextNotLeased_shouldLeaseEntityUntilSave() {
        var negotiation = TestFunctions.createNegotiation("any");
        negotiation.transitionInitial();
        store.save(negotiation);

        var firstQueryResult = store.nextNotLeased(1, hasState(INITIAL.code()));
        assertThat(firstQueryResult).hasSize(1);

        var secondQueryResult = store.nextNotLeased(1, hasState(INITIAL.code()));
        assertThat(secondQueryResult).hasSize(0);

        var retrieved = firstQueryResult.get(0);
        store.save(retrieved);

        var thirdQueryResult = store.nextNotLeased(1, hasState(INITIAL.code()));
        assertThat(thirdQueryResult).hasSize(1);
    }

    @Test
    void verifyMultipleRequest() {
        String id1 = UUID.randomUUID().toString();
        ContractNegotiation negotiation1 = TestFunctions.createNegotiation(id1);
        negotiation1.transitionInitial();
        store.save(negotiation1);

        String id2 = UUID.randomUUID().toString();
        ContractNegotiation negotiation2 = TestFunctions.createNegotiation(id2);
        negotiation2.transitionInitial();
        store.save(negotiation2);


        ContractNegotiation found1 = store.findById(id1);
        assertNotNull(found1);

        ContractNegotiation found2 = store.findById(id2);
        assertNotNull(found2);

        var found = store.nextNotLeased(3, hasState(INITIAL.code()));
        assertEquals(2, found.size());

    }

    @Test
    void verifyOrderingByTimestamp() {
        for (int i = 0; i < 100; i++) {
            ContractNegotiation negotiation = TestFunctions.createNegotiation("test-negotiation-" + i);
            negotiation.transitionInitial();
            store.save(negotiation);
        }

        List<ContractNegotiation> processes = store.nextNotLeased(50, hasState(INITIAL.code()));

        assertThat(processes).hasSize(50);
        assertThat(processes).allMatch(p -> p.getStateTimestamp() > 0);
    }

    @Test
    void nextNotLeased_avoidsStarvation() {
        for (int i = 0; i < 10; i++) {
            ContractNegotiation negotiation = TestFunctions.createNegotiation("test-negotiation-" + i);
            negotiation.transitionInitial();
            store.save(negotiation);
        }

        var list1 = store.nextNotLeased(5, hasState(INITIAL.code()));
        var list2 = store.nextNotLeased(5, hasState(INITIAL.code()));
        assertThat(list1).isNotEqualTo(list2).doesNotContainAnyElementsOf(list2);
    }

    @Test
    void nextNotLeased_typeFilter() {
        range(0, 5).mapToObj(it -> createNegotiationBuilder("1" + it)
                .state(REQUESTED.code())
                .type(PROVIDER)
                .build()).forEach(store::save);
        range(5, 10).mapToObj(it -> createNegotiationBuilder("1" + it)
                .state(REQUESTED.code())
                .type(CONSUMER)
                .build()).forEach(store::save);

        var result = store.nextNotLeased(10, hasState(REQUESTED.code()), new Criterion("type", "=", "CONSUMER"));

        assertThat(result).hasSize(5).allMatch(it -> it.getType() == CONSUMER);
    }

    @Test
    void findAll_noQuerySpec() {
        range(0, 10).forEach(i -> store.save(TestFunctions.createNegotiation("test-neg-" + i)));

        var all = store.queryNegotiations(QuerySpec.Builder.newInstance().build());
        assertThat(all).hasSize(10);
    }

    @Test
    void findAll_verifyPaging() {

        range(0, 10).forEach(i -> store.save(TestFunctions.createNegotiation("test-neg-" + i)));

        // page size fits
        assertThat(store.queryNegotiations(QuerySpec.Builder.newInstance().offset(3).limit(4).build())).hasSize(4);

        // page size too large
        assertThat(store.queryNegotiations(QuerySpec.Builder.newInstance().offset(5).limit(100).build())).hasSize(5);
    }

    @Test
    void findAll_verifyFiltering() {
        range(0, 10).forEach(i -> store.save(TestFunctions.createNegotiation("test-neg-" + i)));
        var querySpec = QuerySpec.Builder.newInstance().filter(criterion("id", "=", "test-neg-3")).build();

        assertThat(store.queryNegotiations(querySpec)).extracting(ContractNegotiation::getId).containsOnly("test-neg-3");
    }

    @Test
    void findAll_verifyFiltering_invalidFilterExpression() {
        range(0, 10).forEach(i -> store.save(TestFunctions.createNegotiation("test-neg-" + i)));
        var querySpec = QuerySpec.Builder.newInstance().filter(criterion("something", "foobar", "other")).build();

        assertThatThrownBy(() -> store.queryNegotiations(querySpec)).isInstanceOfAny(IllegalArgumentException.class);
    }

    @Test
    void findAll_verifySorting() {
        range(0, 10).forEach(i -> store.save(TestFunctions.createNegotiation("test-neg-" + i)));

        assertThat(store.queryNegotiations(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build())).hasSize(10).isSortedAccordingTo(Comparator.comparing(ContractNegotiation::getId));
        assertThat(store.queryNegotiations(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build())).hasSize(10).isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));
    }

    @Test
    void findAll_verifySorting_invalidProperty() {
        range(0, 10).forEach(i -> store.save(TestFunctions.createNegotiation("test-neg-" + i)));
        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

        var result = store.queryNegotiations(query);

        assertThat(result).isEmpty();
    }

    @Test
    void queryAgreements_noQuerySpec() {
        range(0, 10).forEach(i -> {
            var contractAgreement = TestFunctions.createAgreementBuilder().id(ContractId.create(UUID.randomUUID().toString(), TEST_ASSET_ID).toString()).build();
            var negotiation = TestFunctions.createNegotiationBuilder(UUID.randomUUID().toString()).contractAgreement(contractAgreement).build();
            store.save(negotiation);
        });

        var all = store.queryAgreements(QuerySpec.Builder.newInstance().build());

        assertThat(all).hasSize(10);
    }

    @Test
    void queryAgreements_verifyPaging() {
        range(0, 10).forEach(i -> {
            var contractAgreement = TestFunctions.createAgreementBuilder().id(ContractId.create(UUID.randomUUID().toString(), TEST_ASSET_ID).toString()).build();
            var negotiation = TestFunctions.createNegotiationBuilder(UUID.randomUUID().toString()).contractAgreement(contractAgreement).build();
            store.save(negotiation);
        });

        // page size fits
        assertThat(store.queryAgreements(QuerySpec.Builder.newInstance().offset(3).limit(4).build())).hasSize(4);

        // page size too large
        assertThat(store.queryAgreements(QuerySpec.Builder.newInstance().offset(5).limit(100).build())).hasSize(5);
    }

    @Test
    void queryAgreements_verifyFiltering() {
        range(0, 10).forEach(i -> {
            var contractAgreement = TestFunctions.createAgreementBuilder().id(i + ":" + i).build();
            var negotiation = createNegotiationBuilder(UUID.randomUUID().toString()).contractAgreement(contractAgreement).build();
            store.save(negotiation);
        });
        var query = QuerySpec.Builder.newInstance().filter(criterion("id", "=", "3:3")).build();

        var result = store.queryAgreements(query);

        assertThat(result).extracting(ContractAgreement::getId).containsOnly("3:3");
    }

    @Test
    void queryAgreements_verifySorting() {
        range(0, 9).forEach(i -> {
            var contractAgreement = TestFunctions.createAgreementBuilder().id(i + ":" + i).build();
            var negotiation = createNegotiationBuilder(UUID.randomUUID().toString()).contractAgreement(contractAgreement).build();
            store.save(negotiation);
        });

        var queryAsc = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build();
        assertThat(store.queryAgreements(queryAsc)).hasSize(9).isSortedAccordingTo(Comparator.comparing(ContractAgreement::getId));
        var queryDesc = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build();
        assertThat(store.queryAgreements(queryDesc)).hasSize(9).isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));
    }

    @Test
    void queryAgreements_verifySorting_invalidProperty() {
        range(0, 10).forEach(i -> {
            var contractAgreement = TestFunctions.createAgreementBuilder().id(i + ":" + i).build();
            var negotiation = createNegotiationBuilder(UUID.randomUUID().toString()).contractAgreement(contractAgreement).build();
            store.save(negotiation);
        });

        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

        assertThat(store.queryAgreements(query)).isEmpty();
    }

    @Test
    void getNegotiationsWithAgreementOnAsset_negotiationWithAgreement() {
        var agreement = TestFunctions.createAgreementBuilder().id("contract1").build();
        var negotiation = createNegotiationBuilder("negotiation1").contractAgreement(agreement).build();
        var assetId = agreement.getAssetId();

        store.save(negotiation);

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("contractAgreement.assetId", "=", assetId)))
                .build();
        var result = store.queryNegotiations(query).collect(Collectors.toList());


        assertThat(result).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsOnly(negotiation);
    }

    @Test
    void getNegotiationsWithAgreementOnAsset_negotiationWithoutAgreement() {
        var assetId = UUID.randomUUID().toString();
        var negotiation = ContractNegotiation.Builder.newInstance()
                .type(CONSUMER)
                .id("negotiation1")
                .contractAgreement(null)
                .correlationId("corr-negotiation1")
                .state(ContractNegotiationStates.REQUESTED.code())
                .counterPartyAddress("consumer")
                .counterPartyId("consumerId")
                .protocol("protocol")
                .build();

        store.save(negotiation);

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("contractAgreement.assetId", "=", assetId)))
                .build();
        var result = store.queryNegotiations(query).collect(Collectors.toList());

        assertThat(result).isEmpty();
        assertThat(store.queryAgreements(QuerySpec.none())).isEmpty();
    }

    @Test
    void getNegotiationsWithAgreementOnAsset_multipleNegotiationsSameAsset() {
        var assetId = UUID.randomUUID().toString();
        var negotiation1 = createNegotiationBuilder("negotiation1").contractAgreement(TestFunctions.createAgreementBuilder().id("contract1").assetId(assetId).build()).build();
        var negotiation2 = createNegotiationBuilder("negotiation2").contractAgreement(TestFunctions.createAgreementBuilder().id("contract2").assetId(assetId).build()).build();

        store.save(negotiation1);
        store.save(negotiation2);

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("contractAgreement.assetId", "=", assetId)))
                .build();
        var result = store.queryNegotiations(query).collect(Collectors.toList());

        assertThat(result).hasSize(2)
                .extracting(ContractNegotiation::getId).containsExactlyInAnyOrder("negotiation1", "negotiation2");
    }

    @Test
    void findContractAgreement_returnsNullIfAgreementDoesNotExist() {
        store.save(createNegotiationBuilder("negotiation1").build());

        var agreement = store.findContractAgreement("negotiation1");

        assertThat(agreement).isNull();
    }

    /**
     * this test actually overwrites the base one, because the in-mem test sorts the elements by ID, which are Strings,
     * which have a different natural sorting order than ints.
     */
    @Test
    @DisplayName("Verify that paging and sorting is used")
    void queryNegotiations_withPagingAndSorting() {
        var querySpec = QuerySpec.Builder.newInstance()
                .sortField("id")
                .limit(10).offset(5).build();

        range(0, 100)
                .mapToObj(i -> org.eclipse.edc.connector.contract.spi.testfixtures.negotiation.store.TestFunctions.createNegotiation(String.valueOf(i)))
                .forEach(cn -> getContractNegotiationStore().save(cn));

        var result = getContractNegotiationStore().queryNegotiations(querySpec).collect(Collectors.toList());

        Assertions.assertThat(result).hasSize(10)
                .extracting(ContractNegotiation::getId)
                .isSorted();
    }

    @Override
    protected ContractNegotiationStore getContractNegotiationStore() {
        return store;
    }

    @Override
    protected void lockEntity(String negotiationId, String owner, Duration duration) {
        leases.put(negotiationId, new Lease(owner, Clock.systemUTC().millis(), duration.toMillis()));
    }

    @Override
    protected boolean isLockedBy(String negotiationId, String owner) {
        return leases.entrySet().stream().anyMatch(e -> e.getKey().equals(negotiationId) &&
                e.getValue().getLeasedBy().equals(owner) &&
                !isExpired(e.getValue()));
    }

    private boolean isExpired(Lease e) {
        return e.getLeasedAt() + e.getLeaseDuration() < Clock.systemUTC().millis();
    }

    @NotNull
    private ContractNegotiation requestingNegotiation() {
        var negotiation = TestFunctions.createNegotiation(UUID.randomUUID().toString());
        negotiation.transitionInitial();
        negotiation.transitionRequesting();
        return negotiation;
    }


}
