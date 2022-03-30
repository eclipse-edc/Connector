/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *    ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.datamanagement.policy.service;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.api.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.dataspaceconnector.api.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PolicyServiceImplTest {

    private final PolicyStore policyStore = mock(PolicyStore.class);
    private final ContractDefinitionStore contractDefinitionStore = mock(ContractDefinitionStore.class);
    private final TransactionContext dummyTransactionContext = new NoopTransactionContext();

    private final PolicyServiceImpl policyService = new PolicyServiceImpl(dummyTransactionContext, policyStore, contractDefinitionStore);

    @Test
    void findById_shouldRelyOnPolicyStore() {
        when(policyStore.findById("policyId")).thenReturn(createPolicy("policyId"));

        var policy = policyService.findById("policyId");
        var uidTest = policy.getUid();

        String assetId = "policyId";
        assertThat(policy).isNotNull().matches(hasId(assetId));
    }

    @Test
    void query_shouldRelyOnPolicyStore() {
        var policy = createPolicy("policyId");
        when(policyStore.findAll(any(QuerySpec.class))).thenReturn(Stream.of(policy));
        var policies = policyService.query(QuerySpec.none());

        assertThat(policies).hasSize(1).first().matches(hasId("policyId"));
    }

    @Test
    void createPolicy_shouldCreatePolicyIfItDoesNotAlreadyExist() {
        var policy = createPolicy("policyId");
        when(policyStore.findById("policyId")).thenReturn(null);

        var inserted = policyService.create(policy);

        assertThat(inserted.succeeded()).isTrue();
        assertThat(inserted.getContent()).matches(hasId("policyId"));
    }

    @Test
    void createPolicy_shouldNotCreatePolicyIfItAlreadyExists() {
        var policy = createPolicy("policyId");
        when(policyStore.findById("policyId")).thenReturn(policy);

        var inserted = policyService.create(policy);

        assertThat(inserted.succeeded()).isFalse();
    }

    @Test
    void delete_shouldDeletePolicyIfItsNotReferencedByAnyContractDefinition() {
        when(contractDefinitionStore.findAll(any())).thenReturn(Stream.empty()).thenReturn(Stream.empty());
        when(policyStore.delete("policyId")).thenReturn(createPolicy("policyId"));

        var deleted = policyService.delete("policyId");

        assertThat(deleted.succeeded()).isTrue();
        assertThat(deleted.getContent()).matches(hasId("policyId"));
    }

    @Test
    void delete_shouldNotDeleteIfPolicyIsAlreadyPartOfAContractDefinition() {
        var policy = createPolicy("policyId");
        when(policyStore.delete("policyId")).thenReturn(policy);

        ContractDefinition contractDefinition = ContractDefinition.Builder.newInstance()
                .id("A found Contract Definition")
                .accessPolicy(Policy.Builder.newInstance().build())
                .contractPolicy(Policy.Builder.newInstance().build())
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().constraint("left", "op", "right").build())
                .build();

        when(contractDefinitionStore.findAll(any())).thenReturn(Stream.of(contractDefinition));

        var deleted = policyService.delete("policyId");

        assertThat(deleted.failed()).isTrue();
        assertThat(deleted.getFailure().getReason()).isEqualTo(CONFLICT);
    }

    @Test
    void delete_shouldFailIfPolicyDoesNotExist() {
        when(policyStore.delete("assetId")).thenReturn(null);

        var deleted = policyService.delete("policyId");

        assertThat(deleted.failed()).isTrue();
        assertThat(deleted.getFailure().getReason()).isEqualTo(NOT_FOUND);
    }


    @NotNull
    private Predicate<Policy> hasId(String policyId) {
        return it -> policyId.equals(it.getUid());
    }

    private Policy createPolicy(String assetID) {
        return Policy.Builder.newInstance().id(assetID).build();
    }
}
