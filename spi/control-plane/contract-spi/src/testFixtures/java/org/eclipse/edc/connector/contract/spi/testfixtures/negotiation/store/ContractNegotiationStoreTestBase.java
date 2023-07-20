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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - add functionalities
 *
 */

package org.eclipse.edc.connector.contract.spi.testfixtures.negotiation.store;

import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.contract.spi.testfixtures.negotiation.store.TestFunctions.createCallbackAddress;
import static org.eclipse.edc.connector.contract.spi.testfixtures.negotiation.store.TestFunctions.createContract;
import static org.eclipse.edc.connector.contract.spi.testfixtures.negotiation.store.TestFunctions.createContractBuilder;
import static org.eclipse.edc.connector.contract.spi.testfixtures.negotiation.store.TestFunctions.createNegotiation;
import static org.eclipse.edc.connector.contract.spi.testfixtures.negotiation.store.TestFunctions.createNegotiationBuilder;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_LEASED;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;

public abstract class ContractNegotiationStoreTestBase {
    protected static final String CONNECTOR_NAME = "test-connector";
    private static final String ASSET_ID = "TEST_ASSET_ID";

    @Test
    @DisplayName("Verify that an entity is found by ID")
    void find() {
        var id = UUID.randomUUID().toString();
        var negotiation = createNegotiationBuilder(id)
                .pending(true)
                .build();
        getContractNegotiationStore().save(negotiation);

        var result = getContractNegotiationStore().findById(id);

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(negotiation);
    }

    @Test
    @DisplayName("Verify that an entity is found by ID even when leased")
    void find_whenLeased_shouldReturnEntity() {
        var id = "test-cn1";
        var negotiation = createNegotiation(id);
        getContractNegotiationStore().save(negotiation);

        leaseEntity(id, CONNECTOR_NAME);
        assertThat(getContractNegotiationStore().findById(id))
                .usingRecursiveComparison()
                .isEqualTo(negotiation);


        var id2 = "test-cn2";
        var negotiation2 = createNegotiation(id2);
        getContractNegotiationStore().save(negotiation2);

        leaseEntity(id2, "someone-else");
        assertThat(getContractNegotiationStore().findById(id2))
                .usingRecursiveComparison()
                .isEqualTo(negotiation2);

    }

    @Test
    @DisplayName("Verify that null is returned when entity not found")
    void find_notExist() {
        assertThat(getContractNegotiationStore().findById("not-exist")).isNull();
    }

    @Test
    @DisplayName("Find entity by its correlation ID")
    void findForCorrelationId() {
        var negotiation = createNegotiation("test-cn1");
        getContractNegotiationStore().save(negotiation);

        assertThat(getContractNegotiationStore().findForCorrelationId(negotiation.getCorrelationId()))
                .usingRecursiveComparison()
                .isEqualTo(negotiation);
    }

    @Test
    @DisplayName("Find ContractAgreement by contract ID")
    void findContractAgreement() {
        var agreement = createContract(ContractId.create("test-cd1", "test-as1"));
        var negotiation = createNegotiation("test-cn1", agreement);
        getContractNegotiationStore().save(negotiation);

        assertThat(getContractNegotiationStore().findContractAgreement(agreement.getId()))
                .usingRecursiveComparison()
                .isEqualTo(agreement);
    }

    @Test
    @DisplayName("Verify that null is returned if ContractAgreement not found")
    void findContractAgreement_notExist() {
        assertThat(getContractNegotiationStore().findContractAgreement("not-exist")).isNull();
    }

    @Test
    @DisplayName("Verify that entity is stored")
    void save() {
        var negotiation = createNegotiationBuilder("test-id1")
                .type(ContractNegotiation.Type.PROVIDER)
                .build();
        getContractNegotiationStore().save(negotiation);

        assertThat(getContractNegotiationStore().findById(negotiation.getId()))
                .usingRecursiveComparison()
                .isEqualTo(negotiation);
    }

    @Test
    @DisplayName("Verify that entity is stored with callbacks")
    void save_verifyCallbacks() {
        var callbacks = List.of(CallbackAddress.Builder.newInstance().uri("test").events(Set.of("event")).build());

        var negotiation = createNegotiationBuilder("test-id1")
                .type(ContractNegotiation.Type.CONSUMER)
                .callbackAddresses(callbacks)
                .build();

        getContractNegotiationStore().save(negotiation);

        var contract = getContractNegotiationStore().findById(negotiation.getId());

        assertThat(contract)
                .isNotNull()
                .usingRecursiveComparison()
                .isEqualTo(negotiation);

        assertThat(contract.getCallbackAddresses()).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsAll(callbacks);
    }

    @Test
    @DisplayName("Verify that entity and related entities are stored")
    void save_withContract() {
        var agreement = createContract(ContractId.create("definition", "asset"));
        var negotiation = createNegotiation("test-negotiation", agreement);
        getContractNegotiationStore().save(negotiation);

        var actual = getContractNegotiationStore().findById(negotiation.getId());
        assertThat(actual)
                .isNotNull()
                .usingRecursiveComparison()
                .isEqualTo(negotiation);
        assertThat(actual.getContractAgreement()).usingRecursiveComparison().isEqualTo(agreement);
    }

    @Test
    @DisplayName("Verify that an existing entity is updated instead")
    void save_exists_shouldUpdate() {
        var id = "test-id1";
        var negotiation = createNegotiation(id);
        getContractNegotiationStore().save(negotiation);

        var newNegotiation = ContractNegotiation.Builder.newInstance()
                .type(ContractNegotiation.Type.CONSUMER)
                .id(id)
                .stateCount(420) //modified
                .state(800) //modified
                .correlationId("corr-test-id1")
                .counterPartyAddress("consumer")
                .counterPartyId("consumerId")
                .protocol("protocol")
                .build();

        getContractNegotiationStore().save(newNegotiation);

        var actual = getContractNegotiationStore().findById(negotiation.getId());
        assertThat(actual).isNotNull();
        assertThat(actual.getStateCount()).isEqualTo(420);
        assertThat(actual.getState()).isEqualTo(800);
    }

    @Test
    @DisplayName("Verify that updating an entity breaks the lease (if lease by self)")
    void update_leasedBySelf_shouldBreakLease() {
        var id = "test-id1";
        var builder = createNegotiationBuilder(id);
        var negotiation = builder.build();
        getContractNegotiationStore().save(negotiation);

        leaseEntity(id, CONNECTOR_NAME);

        var newNegotiation = builder
                .stateCount(420) //modified
                .state(800) //modified
                .updatedAt(Clock.systemUTC().millis())
                .build();

        // update should break lease
        getContractNegotiationStore().save(newNegotiation);

        assertThat(isLeasedBy(id, CONNECTOR_NAME)).isFalse();

        var next = getContractNegotiationStore().nextNotLeased(10, hasState(800));
        assertThat(next).usingRecursiveFieldByFieldElementComparatorIgnoringFields("updatedAt").containsOnly(newNegotiation);

    }

    @Test
    @DisplayName("Verify that updating an entity throws an exception if leased by someone else")
    void update_leasedByOther_shouldThrowException() {
        var id = "test-id1";
        var negotiation = createNegotiation(id);
        getContractNegotiationStore().save(negotiation);

        leaseEntity(id, "someone-else");

        var newNegotiation = ContractNegotiation.Builder.newInstance()
                .type(ContractNegotiation.Type.CONSUMER)
                .id(id)
                .stateCount(420) //modified
                .state(800) //modified
                .correlationId("corr-test-id1")
                .counterPartyAddress("consumer")
                .counterPartyId("consumerId")
                .protocol("protocol")
                .build();

        // update should break lease
        assertThat(isLeasedBy(id, "someone-else")).isTrue();
        assertThatThrownBy(() -> getContractNegotiationStore().save(newNegotiation)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Should persist the agreement when a negotiation is updated")
    void update_addsAgreement_shouldPersist() {
        var negotiationId = "test-cn1";
        var negotiation = createNegotiation(negotiationId);
        getContractNegotiationStore().save(negotiation);

        // now add the agreement
        var agreement = createContract(ContractId.create("definition", "asset"));
        var updatedNegotiation = createNegotiation(negotiationId, agreement);

        getContractNegotiationStore().save(updatedNegotiation); //should perform an update + insert

        assertThat(getContractNegotiationStore().queryAgreements(QuerySpec.none()))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(agreement);

        assertThat(Objects.requireNonNull(getContractNegotiationStore().findById(negotiationId)).getContractAgreement()).usingRecursiveComparison().isEqualTo(agreement);
    }

    @Test
    void update_changeFields() {
        var negotiationId = UUID.randomUUID().toString();
        var builder = createNegotiationBuilder(negotiationId)
                .callbackAddresses(List.of(createCallbackAddress()))
                .pending(false);
        getContractNegotiationStore().save(builder.build());

        var inserted = getContractNegotiationStore().findById(negotiationId);
        assertThat(inserted).isNotNull().satisfies(i -> {
            assertThat(i.getCallbackAddresses()).hasSize(1);
            assertThat(i.isPending()).isFalse();
        });

        builder.callbackAddresses(emptyList()).pending(true);

        getContractNegotiationStore().save(builder.build());

        var updated = getContractNegotiationStore().findById(negotiationId);
        assertThat(updated).isNotNull().satisfies(u -> {
            assertThat(u.getCallbackAddresses()).isEmpty();
            assertThat(u.isPending()).isTrue();
        });
    }

    @Test
    void create_and_cancel_contractAgreement() {
        var negotiationId = "test-cn1";
        var negotiation = createNegotiation(negotiationId);
        getContractNegotiationStore().save(negotiation);

        // now add the agreement
        var agreement = createContract(ContractId.create("definition", "asset"));
        var updatedNegotiation = createNegotiation(negotiationId, agreement);

        getContractNegotiationStore().save(updatedNegotiation);
        assertThat(getContractNegotiationStore().queryAgreements(QuerySpec.none()))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(agreement);

        // cancel the agreement
        updatedNegotiation.transitionTerminating("Cancelled");
        getContractNegotiationStore().save(updatedNegotiation);
    }

    @Test
    @DisplayName("Should update the agreement when a negotiation is updated")
    void update_whenAgreementExists_shouldUpdate() {
        var negotiationId = "test-cn1";
        var agreement = createContract(ContractId.create("definition", "asset"));
        var negotiation = createNegotiation(negotiationId, null);
        getContractNegotiationStore().save(negotiation);
        var dbNegotiation = getContractNegotiationStore().findById(negotiationId);
        assertThat(dbNegotiation).isNotNull().satisfies(n ->
                assertThat(n.getContractAgreement()).isNull()
        );

        dbNegotiation.setContractAgreement(agreement);
        getContractNegotiationStore().save(dbNegotiation);

        var updatedNegotiation = getContractNegotiationStore().findById(negotiationId);
        assertThat(updatedNegotiation).isNotNull();
        assertThat(updatedNegotiation.getContractAgreement()).isNotNull();
    }

    @Test
    @DisplayName("Verify that an entity can be deleted")
    void delete() {
        var id = UUID.randomUUID().toString();
        var n = createNegotiation(id);
        getContractNegotiationStore().save(n);

        assertThat(getContractNegotiationStore().findById(id)).isNotNull().usingRecursiveComparison().isEqualTo(n);

        //todo: verify returned object
        getContractNegotiationStore().delete(id);

        assertThat(getContractNegotiationStore().findById(id)).isNull();
    }

    @Test
    @DisplayName("Verify that an entity cannot be deleted when leased by self")
    void delete_whenLeasedBySelf_shouldThrowException() {
        var id = UUID.randomUUID().toString();
        var n = createNegotiation(id);
        getContractNegotiationStore().save(n);

        leaseEntity(id, CONNECTOR_NAME);

        //todo: verify returned object
        assertThatThrownBy(() -> getContractNegotiationStore().delete(id)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Verify that an entity cannot be deleted when leased by other")
    void delete_whenLeasedByOther_shouldThrowException() {
        var id = UUID.randomUUID().toString();
        var n = createNegotiation(id);
        getContractNegotiationStore().save(n);

        leaseEntity(id, "someone-else");

        //todo: verify returned object
        assertThatThrownBy(() -> getContractNegotiationStore().delete(id)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Verify that deleting a non-existing entity returns null")
    void delete_notExist() {
        // todo: verify returned object is null
        getContractNegotiationStore().delete("not-exist");
    }

    @Test
    @DisplayName("Verify that attempting to delete a negotiation with a contract raises an exception")
    void delete_contractExists() {
        var id = UUID.randomUUID().toString();
        var contract = createContract(ContractId.create("definition", "asset"));
        var n = createNegotiation(id, contract);
        getContractNegotiationStore().save(n);

        assertThat(getContractNegotiationStore().findById(id)).isNotNull().usingRecursiveComparison().isEqualTo(n);
        assertThatThrownBy(() -> getContractNegotiationStore().delete(id)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete ContractNegotiation")
                .hasMessageContaining("ContractAgreement already created.");

    }

    @Test
    @DisplayName("Verify that paging is used")
    void queryNegotiations() {
        var querySpec = QuerySpec.Builder.newInstance()
                .limit(10).offset(5).build();

        IntStream.range(0, 100)
                .mapToObj(i -> createNegotiation(String.valueOf(i)))
                .forEach(cn -> getContractNegotiationStore().save(cn));

        var result = getContractNegotiationStore().queryNegotiations(querySpec);

        assertThat(result).hasSize(10);
    }

    @Test
    @DisplayName("Verify that paging and sorting is used")
    void queryNegotiations_withPagingAndSorting() {
        var querySpec = QuerySpec.Builder.newInstance()
                .sortField("id")
                .limit(10).offset(5).build();

        IntStream.range(0, 100)
                .mapToObj(i -> createNegotiation(String.valueOf(i)))
                .forEach(cn -> getContractNegotiationStore().save(cn));

        var result = getContractNegotiationStore().queryNegotiations(querySpec);

        assertThat(result).hasSize(10)
                .extracting(ContractNegotiation::getId)
                .isSorted();
    }

    @Test
    void getNegotiationsWithAgreementOnAsset_negotiationWithAgreement() {
        var agreement = createContract(ContractId.create("definition", "asset"));
        var negotiation = createNegotiation("negotiation1", agreement);
        var assetId = agreement.getAssetId();

        getContractNegotiationStore().save(negotiation);

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("contractAgreement.assetId", "=", assetId)))
                .build();
        var result = getContractNegotiationStore().queryNegotiations(query);

        assertThat(result).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsOnly(negotiation);

    }

    @Test
    void getNegotiationsWithAgreementOnAsset_negotiationWithoutAgreement() {
        var assetId = UUID.randomUUID().toString();
        var negotiation = createNegotiation("negotiation1");

        getContractNegotiationStore().save(negotiation);

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("contractAgreement.assetId", "=", assetId)))
                .build();
        var result = getContractNegotiationStore().queryNegotiations(query);

        assertThat(result).isEmpty();
        assertThat(getContractNegotiationStore().queryAgreements(QuerySpec.none())).isEmpty();

    }

    @Test
    void getNegotiationsWithAgreementOnAsset_multipleNegotiationsSameAsset() {
        var assetId = UUID.randomUUID().toString();
        var negotiation1 = createNegotiation("negotiation1", createContractBuilder("contract1").assetId(assetId).build());
        var negotiation2 = createNegotiation("negotiation2", createContractBuilder("contract2").assetId(assetId).build());

        getContractNegotiationStore().save(negotiation1);
        getContractNegotiationStore().save(negotiation2);

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("contractAgreement.assetId", "=", assetId)))
                .build();
        var result = getContractNegotiationStore().queryNegotiations(query);

        assertThat(result).hasSize(2)
                .extracting(ContractNegotiation::getId).containsExactlyInAnyOrder("negotiation1", "negotiation2");

    }

    @Test
    @DisplayName("Verify that paging is used (with ContractAgreement)")
    void queryNegotiations_withAgreement() {
        var querySpec = QuerySpec.Builder.newInstance().limit(10).offset(5).build();

        IntStream.range(0, 100)
                .mapToObj(i -> {
                    var agreement = createContract(ContractId.create("definition" + 1, "asset"));
                    return createNegotiation(String.valueOf(i), agreement);
                })
                .forEach(cn -> getContractNegotiationStore().save(cn));

        var result = getContractNegotiationStore().queryNegotiations(querySpec);

        assertThat(result).hasSize(10);
    }

    @Test
    @DisplayName("Verify that out-of-bounds paging parameters produce empty result")
    void queryNegotiations_offsetTooLarge() {
        var querySpec = QuerySpec.Builder.newInstance().limit(10).offset(50).build();

        IntStream.range(0, 10)
                .mapToObj(i -> createNegotiation(String.valueOf(i)))
                .forEach(cn -> getContractNegotiationStore().save(cn));

        var result = getContractNegotiationStore().queryNegotiations(querySpec);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Verify that nextNotLeased returns the correct amount of items")
    void nextNotLeased() {
        var negotiations = IntStream
                .range(0, 10)
                .mapToObj(i -> createNegotiation("id" + i))
                .collect(Collectors.toList());
        negotiations.forEach(getContractNegotiationStore()::save);

        var batch = getContractNegotiationStore().nextNotLeased(5, hasState(REQUESTED.code()));

        assertThat(batch).hasSize(5).isSubsetOf(negotiations);

    }

    @Test
    @DisplayName("nextNotLeased: verify that only non-leased entities are returned")
    void nextNotLeased_withLeasedEntity() {
        var negotiations = IntStream
                .range(0, 10)
                .mapToObj(i -> createNegotiation(String.valueOf(i)))
                .collect(Collectors.toList());
        negotiations.forEach(getContractNegotiationStore()::save);

        // mark a few as "leased"
        getContractNegotiationStore().nextNotLeased(5, hasState(REQUESTED.code()));

        var batch2 = getContractNegotiationStore().nextNotLeased(10, hasState(REQUESTED.code()));
        assertThat(batch2)
                .hasSize(5)
                .isSubsetOf(negotiations)
                .extracting(ContractNegotiation::getId)
                .map(Integer::parseInt)
                .allMatch(i -> i >= 5);
    }

    @Test
    @DisplayName("nextNotLeased: verify that an expired lease is re-acquired")
    void nextNotLeased_withLeasedEntity_expiredLease() throws InterruptedException {
        var negotiations = IntStream
                .range(0, 5)
                .mapToObj(i -> createNegotiation(String.valueOf(i)))
                .collect(Collectors.toList());
        negotiations.forEach(getContractNegotiationStore()::save);

        // mark them as "leased"
        negotiations.forEach(n -> leaseEntity(n.getId(), CONNECTOR_NAME, Duration.ofMillis(10)));

        // let enough time pass
        Thread.sleep(50);

        var leasedNegotiations = getContractNegotiationStore().nextNotLeased(5, hasState(REQUESTED.code()));
        assertThat(leasedNegotiations)
                .hasSize(5)
                .containsAll(negotiations);

        assertThat(leasedNegotiations).allMatch(n -> isLeasedBy(n.getId(), CONNECTOR_NAME));
    }

    @Test
    @DisplayName("Verify that nextNotLeased returns the agreement")
    void nextNotLeased_withAgreement() {
        var contractAgreement = createContract(ContractId.create(UUID.randomUUID().toString(), ASSET_ID));
        var negotiation = createNegotiationBuilder(UUID.randomUUID().toString())
                .contractAgreement(contractAgreement)
                .state(ContractNegotiationStates.AGREED.code())
                .build();

        getContractNegotiationStore().save(negotiation);

        var batch = getContractNegotiationStore().nextNotLeased(1, hasState(ContractNegotiationStates.AGREED.code()));

        assertThat(batch).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(negotiation);

    }

    @Test
    void queryAgreements_noQuerySpec() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContract(ContractId.create(UUID.randomUUID().toString(), ASSET_ID));
            var negotiation = createNegotiation(UUID.randomUUID().toString(), contractAgreement);
            getContractNegotiationStore().save(negotiation);
        });

        var all = getContractNegotiationStore().queryAgreements(QuerySpec.Builder.newInstance().build());

        assertThat(all).hasSize(10);
    }

    @Test
    void queryAgreements_verifyPaging() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContract(ContractId.create(UUID.randomUUID().toString(), ASSET_ID));
            var negotiation = createNegotiation(UUID.randomUUID().toString(), contractAgreement);
            getContractNegotiationStore().save(negotiation);
        });

        // page size fits
        assertThat(getContractNegotiationStore().queryAgreements(QuerySpec.Builder.newInstance().offset(3).limit(4).build())).hasSize(4);

        // page size too large
        assertThat(getContractNegotiationStore().queryAgreements(QuerySpec.Builder.newInstance().offset(5).limit(100).build())).hasSize(5);
    }

    @Test
    void findByIdAndLease_shouldReturnTheEntityAndLeaseIt() {
        var id = UUID.randomUUID().toString();
        getContractNegotiationStore().save(createNegotiation(id));

        var result = getContractNegotiationStore().findByIdAndLease(id);

        assertThat(result).isSucceeded();
        assertThat(isLeasedBy(id, CONNECTOR_NAME)).isTrue();
    }

    @Test
    void findByIdAndLease_shouldReturnNotFound_whenEntityDoesNotExist() {
        var result = getContractNegotiationStore().findByIdAndLease("unexistent");

        assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(NOT_FOUND);
    }

    @Test
    void findByIdAndLease_shouldReturnAlreadyLeased_whenEntityIsAlreadyLeased() {
        var id = UUID.randomUUID().toString();
        getContractNegotiationStore().save(createNegotiation(id));
        leaseEntity(id, "other owner");

        var result = getContractNegotiationStore().findByIdAndLease(id);

        assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(ALREADY_LEASED);
    }

    @Test
    void findByCorrelationIdAndLease_shouldReturnTheEntityAndLeaseIt() {
        var id = UUID.randomUUID().toString();
        var correlationId = UUID.randomUUID().toString();
        getContractNegotiationStore().save(createNegotiationBuilder(id).correlationId(correlationId).build());

        var result = getContractNegotiationStore().findByCorrelationIdAndLease(correlationId);

        assertThat(result).isSucceeded();
        assertThat(isLeasedBy(id, CONNECTOR_NAME)).isTrue();
    }

    @Test
    void findByCorrelationIdAndLease_shouldReturnNotFound_whenEntityDoesNotExist() {
        var result = getContractNegotiationStore().findByCorrelationIdAndLease("unexistent");

        assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(NOT_FOUND);
    }

    @Test
    void findByCorrelationIdAndLease_shouldReturnAlreadyLeased_whenEntityIsAlreadyLeased() {
        var id = UUID.randomUUID().toString();
        var correlationId = UUID.randomUUID().toString();
        getContractNegotiationStore().save(createNegotiationBuilder(id).correlationId(correlationId).build());
        leaseEntity(id, "other owner");

        var result = getContractNegotiationStore().findByCorrelationIdAndLease(correlationId);

        assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(ALREADY_LEASED);
    }

    protected abstract ContractNegotiationStore getContractNegotiationStore();

    protected abstract void leaseEntity(String negotiationId, String owner, Duration duration);

    protected void leaseEntity(String negotiationId, String owner) {
        leaseEntity(negotiationId, owner, Duration.ofSeconds(60));
    }

    protected abstract boolean isLeasedBy(String negotiationId, String owner);

}
