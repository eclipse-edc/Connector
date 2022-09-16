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

package org.eclipse.dataspaceconnector.spi.contract.negotiation.store;

import org.eclipse.dataspaceconnector.spi.contract.ContractId;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class ContractNegotiationStoreTestBase {
    protected static final String CONNECTOR_NAME = "test-connector";

    @Test
    @DisplayName("Verify that an entity is found by ID")
    void find() {
        var id = "test-cn1";
        var negotiation = TestFunctions.createNegotiation(id);
        getContractNegotiationStore().save(negotiation);

        assertThat(getContractNegotiationStore().find(id))
                .usingRecursiveComparison()
                .isEqualTo(negotiation);

    }

    @Test
    @DisplayName("Verify that an entity is found by ID even when leased")
    void find_whenLeased_shouldReturnEntity() {
        var id = "test-cn1";
        var negotiation = TestFunctions.createNegotiation(id);
        getContractNegotiationStore().save(negotiation);

        lockEntity(id, CONNECTOR_NAME);
        assertThat(getContractNegotiationStore().find(id))
                .usingRecursiveComparison()
                .isEqualTo(negotiation);


        var id2 = "test-cn2";
        var negotiation2 = TestFunctions.createNegotiation(id2);
        getContractNegotiationStore().save(negotiation2);

        lockEntity(id2, "someone-else");
        assertThat(getContractNegotiationStore().find(id2))
                .usingRecursiveComparison()
                .isEqualTo(negotiation2);

    }

    @Test
    @DisplayName("Verify that null is returned when entity not found")
    void find_notExist() {
        assertThat(getContractNegotiationStore().find("not-exist")).isNull();
    }

    @Test
    @DisplayName("Find entity by its correlation ID")
    void findForCorrelationId() {
        var negotiation = TestFunctions.createNegotiation("test-cn1");
        getContractNegotiationStore().save(negotiation);

        assertThat(getContractNegotiationStore().findForCorrelationId(negotiation.getCorrelationId()))
                .usingRecursiveComparison()
                .isEqualTo(negotiation);
    }

    @Test
    @DisplayName("Find ContractAgreement by contract ID")
    void findContractAgreement() {
        var agreement = TestFunctions.createContract("test-ca1");
        var negotiation = TestFunctions.createNegotiation("test-cn1", agreement);
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
        var negotiation = TestFunctions.createNegotiationBuilder("test-id1")
                .type(ContractNegotiation.Type.PROVIDER)
                .build();
        getContractNegotiationStore().save(negotiation);

        assertThat(getContractNegotiationStore().find(negotiation.getId()))
                .usingRecursiveComparison()
                .isEqualTo(negotiation);
    }

    @Test
    @DisplayName("Verify that entity and related entities are stored")
    void save_withContract() {
        var agreement = TestFunctions.createContract("test-agreement");
        var negotiation = TestFunctions.createNegotiation("test-negotiation", agreement);
        getContractNegotiationStore().save(negotiation);

        var actual = getContractNegotiationStore().find(negotiation.getId());
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
        var negotiation = TestFunctions.createNegotiation(id);
        getContractNegotiationStore().save(negotiation);

        var newNegotiation = ContractNegotiation.Builder.newInstance()
                .type(ContractNegotiation.Type.CONSUMER)
                .id(id)
                .stateCount(420) //modified
                .state(800) //modified
                .correlationId("corr-test-id1")
                .counterPartyAddress("consumer")
                .counterPartyId("consumerId")
                .protocol("ids-multipart")
                .build();

        getContractNegotiationStore().save(newNegotiation);

        var actual = getContractNegotiationStore().find(negotiation.getId());
        assertThat(actual).isNotNull();
        assertThat(actual.getStateCount()).isEqualTo(420);
        assertThat(actual.getState()).isEqualTo(800);
    }

    @Test
    @DisplayName("Verify that updating an entity breaks the lease (if lease by self)")
    void update_leasedBySelf_shouldBreakLease() {
        var id = "test-id1";
        var builder = TestFunctions.createNegotiationBuilder(id);
        var negotiation = builder.build();
        getContractNegotiationStore().save(negotiation);

        lockEntity(id, CONNECTOR_NAME);

        var newNegotiation = builder
                .stateCount(420) //modified
                .state(800) //modified
                .updatedAt(Clock.systemUTC().millis())
                .build();

        // update should break lease
        getContractNegotiationStore().save(newNegotiation);

        assertThat(isLockedBy(id, CONNECTOR_NAME)).isFalse();

        var next = getContractNegotiationStore().nextForState(800, 10);
        assertThat(next).usingRecursiveFieldByFieldElementComparatorIgnoringFields("updatedAt").containsOnly(newNegotiation);

    }

    @Test
    @DisplayName("Verify that updating an entity throws an exception if leased by someone else")
    void update_leasedByOther_shouldThrowException() {
        var id = "test-id1";
        var negotiation = TestFunctions.createNegotiation(id);
        getContractNegotiationStore().save(negotiation);

        lockEntity(id, "someone-else");

        var newNegotiation = ContractNegotiation.Builder.newInstance()
                .type(ContractNegotiation.Type.CONSUMER)
                .id(id)
                .stateCount(420) //modified
                .state(800) //modified
                .correlationId("corr-test-id1")
                .counterPartyAddress("consumer")
                .counterPartyId("consumerId")
                .protocol("ids-multipart")
                .build();

        // update should break lease
        assertThat(isLockedBy(id, "someone-else")).isTrue();
        assertThatThrownBy(() -> getContractNegotiationStore().save(newNegotiation)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Should persist the agreement when a negotiation is updated")
    void update_addsAgreement_shouldPersist() {
        var negotiationId = "test-cn1";
        var negotiation = TestFunctions.createNegotiation(negotiationId);
        getContractNegotiationStore().save(negotiation);

        // now add the agreement
        var agreement = TestFunctions.createContract("test-ca1");
        var updatedNegotiation = TestFunctions.createNegotiation(negotiationId, agreement);

        getContractNegotiationStore().save(updatedNegotiation); //should perform an update + insert

        assertThat(getContractNegotiationStore().queryAgreements(QuerySpec.none()))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(agreement);

        assertThat(Objects.requireNonNull(getContractNegotiationStore().find(negotiationId)).getContractAgreement()).usingRecursiveComparison().isEqualTo(agreement);
    }

    @Test
    void create_and_cancel_contractAgreement() {
        var negotiationId = "test-cn1";
        var negotiation = TestFunctions.createNegotiation(negotiationId);
        getContractNegotiationStore().save(negotiation);

        // now add the agreement
        var agreement = TestFunctions.createContract("test-ca1");
        var updatedNegotiation = TestFunctions.createNegotiation(negotiationId, agreement);

        getContractNegotiationStore().save(updatedNegotiation);
        assertThat(getContractNegotiationStore().queryAgreements(QuerySpec.none()))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(agreement);

        // cancel the agreement
        updatedNegotiation.transitionError("Cancelled");
        getContractNegotiationStore().save(updatedNegotiation);
    }

    @Test
    @DisplayName("Should update the agreement when a negotiation is updated")
    void update_whenAgreementExists_shouldUpdate() {
        var negotiationId = "test-cn1";
        var agreement = TestFunctions.createContract("test-ca1");
        var negotiation = TestFunctions.createNegotiation(negotiationId, null);
        getContractNegotiationStore().save(negotiation);
        var dbNegotiation = getContractNegotiationStore().find(negotiationId);
        assertThat(dbNegotiation).isNotNull().satisfies(n ->
                assertThat(n.getContractAgreement()).isNull()
        );

        dbNegotiation.setContractAgreement(agreement);
        getContractNegotiationStore().save(dbNegotiation);

        var updatedNegotiation = getContractNegotiationStore().find(negotiationId);
        assertThat(updatedNegotiation).isNotNull();
        assertThat(updatedNegotiation.getContractAgreement()).isNotNull();
    }

    @Test
    @DisplayName("Verify that an entity can be deleted")
    void delete() {
        var id = UUID.randomUUID().toString();
        var n = TestFunctions.createNegotiation(id);
        getContractNegotiationStore().save(n);

        assertThat(getContractNegotiationStore().find(id)).isNotNull().usingRecursiveComparison().isEqualTo(n);

        //todo: verify returned object
        getContractNegotiationStore().delete(id);

        assertThat(getContractNegotiationStore().find(id)).isNull();
    }

    @Test
    @DisplayName("Verify that an entity cannot be deleted when leased by self")
    void delete_whenLeasedBySelf_shouldThrowException() {
        var id = UUID.randomUUID().toString();
        var n = TestFunctions.createNegotiation(id);
        getContractNegotiationStore().save(n);

        lockEntity(id, CONNECTOR_NAME);

        //todo: verify returned object
        assertThatThrownBy(() -> getContractNegotiationStore().delete(id)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Verify that an entity cannot be deleted when leased by other")
    void delete_whenLeasedByOther_shouldThrowException() {
        var id = UUID.randomUUID().toString();
        var n = TestFunctions.createNegotiation(id);
        getContractNegotiationStore().save(n);

        lockEntity(id, "someone-else");

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
        var contract = TestFunctions.createContract("test-agreement");
        var n = TestFunctions.createNegotiation(id, contract);
        getContractNegotiationStore().save(n);

        assertThat(getContractNegotiationStore().find(id)).isNotNull().usingRecursiveComparison().isEqualTo(n);
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
                .mapToObj(i -> TestFunctions.createNegotiation("" + i))
                .forEach(cn -> getContractNegotiationStore().save(cn));

        var result = getContractNegotiationStore().queryNegotiations(querySpec).collect(Collectors.toList());

        assertThat(result).hasSize(10);
    }

    @Test
    @DisplayName("Verify that paging and sorting is used")
    void queryNegotiations_withPagingAndSorting() {
        var querySpec = QuerySpec.Builder.newInstance()
                .sortField("id")
                .limit(10).offset(5).build();

        IntStream.range(0, 100)
                .mapToObj(i -> TestFunctions.createNegotiation("" + i))
                .forEach(cn -> getContractNegotiationStore().save(cn));

        var result = getContractNegotiationStore().queryNegotiations(querySpec).collect(Collectors.toList());

        assertThat(result).hasSize(10)
                .extracting(ContractNegotiation::getId)
                .isSorted();
    }

    @Test
    void getNegotiationsWithAgreementOnAsset_negotiationWithAgreement() {
        var agreement = TestFunctions.createContract("contract1");
        var negotiation = TestFunctions.createNegotiation("negotiation1", agreement);
        var assetId = agreement.getAssetId();

        getContractNegotiationStore().save(negotiation);

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("contractAgreement.assetId", "=", assetId)))
                .build();
        var result = getContractNegotiationStore().queryNegotiations(query).collect(Collectors.toList());

        assertThat(result).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsOnly(negotiation);

    }

    @Test
    void getNegotiationsWithAgreementOnAsset_negotiationWithoutAgreement() {
        var assetId = UUID.randomUUID().toString();
        var negotiation = TestFunctions.createNegotiation("negotiation1");

        getContractNegotiationStore().save(negotiation);

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("contractAgreement.assetId", "=", assetId)))
                .build();
        var result = getContractNegotiationStore().queryNegotiations(query).collect(Collectors.toList());

        assertThat(result).isEmpty();
        assertThat(getContractNegotiationStore().queryAgreements(QuerySpec.none())).isEmpty();

    }

    @Test
    void getNegotiationsWithAgreementOnAsset_multipleNegotiationsSameAsset() {
        var assetId = UUID.randomUUID().toString();
        var negotiation1 = TestFunctions.createNegotiation("negotiation1", TestFunctions.createContractBuilder("contract1").assetId(assetId).build());
        var negotiation2 = TestFunctions.createNegotiation("negotiation2", TestFunctions.createContractBuilder("contract2").assetId(assetId).build());

        getContractNegotiationStore().save(negotiation1);
        getContractNegotiationStore().save(negotiation2);

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("contractAgreement.assetId", "=", assetId)))
                .build();
        var result = getContractNegotiationStore().queryNegotiations(query).collect(Collectors.toList());

        assertThat(result).hasSize(2)
                .extracting(ContractNegotiation::getId).containsExactlyInAnyOrder("negotiation1", "negotiation2");

    }

    @Test
    @DisplayName("Verify that paging is used (with ContractAgreement)")
    void queryNegotiations_withAgreement() {
        var querySpec = QuerySpec.Builder.newInstance().limit(10).offset(5).build();

        IntStream.range(0, 100)
                .mapToObj(i -> {
                    var agreement = TestFunctions.createContract("contract" + i);
                    return TestFunctions.createNegotiation("" + i, agreement);
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
                .mapToObj(i -> TestFunctions.createNegotiation("" + i))
                .forEach(cn -> getContractNegotiationStore().save(cn));

        var result = getContractNegotiationStore().queryNegotiations(querySpec);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Verify that nextForState returns the correct amount of items")
    void nextForState() {
        var negotiations = IntStream
                .range(0, 10)
                .mapToObj(i -> TestFunctions.createNegotiation("id" + i))
                .collect(Collectors.toList());
        negotiations.forEach(getContractNegotiationStore()::save);

        var batch = getContractNegotiationStore().nextForState(ContractNegotiationStates.REQUESTED.code(), 5);

        assertThat(batch).hasSize(5).isSubsetOf(negotiations);

    }

    @Test
    @DisplayName("nextForState: verify that only non-leased entities are returned")
    void nextForState_withLeasedEntity() {
        var negotiations = IntStream
                .range(0, 10)
                .mapToObj(i -> TestFunctions.createNegotiation(String.valueOf(i)))
                .collect(Collectors.toList());
        negotiations.forEach(getContractNegotiationStore()::save);

        // mark a few as "leased"
        getContractNegotiationStore().nextForState(ContractNegotiationStates.REQUESTED.code(), 5);

        var batch2 = getContractNegotiationStore().nextForState(ContractNegotiationStates.REQUESTED.code(), 10);
        assertThat(batch2)
                .hasSize(5)
                .isSubsetOf(negotiations)
                .extracting(ContractNegotiation::getId)
                .map(Integer::parseInt)
                .allMatch(i -> i >= 5);
    }

    @Test
    @DisplayName("nextForState: verify that an expired lease is re-acquired")
    void nextForState_withLeasedEntity_expiredLease() throws InterruptedException {
        var negotiations = IntStream
                .range(0, 5)
                .mapToObj(i -> TestFunctions.createNegotiation(String.valueOf(i)))
                .collect(Collectors.toList());
        negotiations.forEach(getContractNegotiationStore()::save);

        // mark them as "leased"
        negotiations.forEach(n -> lockEntity(n.getId(), CONNECTOR_NAME, Duration.ofMillis(10)));

        // let enough time pass
        Thread.sleep(50);

        var leasedNegotiations = getContractNegotiationStore().nextForState(ContractNegotiationStates.REQUESTED.code(), 5);
        assertThat(leasedNegotiations)
                .hasSize(5)
                .containsAll(negotiations);

        assertThat(leasedNegotiations).allMatch(n -> isLockedBy(n.getId(), CONNECTOR_NAME));
    }

    @Test
    void getAgreementsForDefinitionId() {
        var contractAgreement = TestFunctions.createContract(ContractId.createContractId("definitionId"));
        var negotiation = TestFunctions.createNegotiation(UUID.randomUUID().toString(), contractAgreement);
        getContractNegotiationStore().save(negotiation);

        var result = getContractNegotiationStore().getAgreementsForDefinitionId("definitionId");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAgreementsForDefinitionId_notFound() {
        var contractAgreement = TestFunctions.createContract(ContractId.createContractId("otherDefinitionId"));
        var negotiation = TestFunctions.createNegotiation(UUID.randomUUID().toString(), contractAgreement);
        getContractNegotiationStore().save(negotiation);

        var result = getContractNegotiationStore().getAgreementsForDefinitionId("definitionId");

        assertThat(result).isEmpty();
    }

    @Test
    void queryAgreements_noQuerySpec() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = TestFunctions.createContract(ContractId.createContractId(UUID.randomUUID().toString()));
            var negotiation = TestFunctions.createNegotiation(UUID.randomUUID().toString(), contractAgreement);
            getContractNegotiationStore().save(negotiation);
        });

        var all = getContractNegotiationStore().queryAgreements(QuerySpec.Builder.newInstance().build());

        assertThat(all).hasSize(10);
    }

    @Test
    void queryAgreements_verifyPaging() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = TestFunctions.createContract(ContractId.createContractId(UUID.randomUUID().toString()));
            var negotiation = TestFunctions.createNegotiation(UUID.randomUUID().toString(), contractAgreement);
            getContractNegotiationStore().save(negotiation);
        });

        // page size fits
        assertThat(getContractNegotiationStore().queryAgreements(QuerySpec.Builder.newInstance().offset(3).limit(4).build())).hasSize(4);

        // page size too large
        assertThat(getContractNegotiationStore().queryAgreements(QuerySpec.Builder.newInstance().offset(5).limit(100).build())).hasSize(5);
    }

    protected abstract ContractNegotiationStore getContractNegotiationStore();

    protected abstract void lockEntity(String negotiationId, String owner, Duration duration);

    protected void lockEntity(String negotiationId, String owner) {
        lockEntity(negotiationId, owner, Duration.ofSeconds(60));
    }

    protected abstract boolean isLockedBy(String negotiationId, String owner);

}