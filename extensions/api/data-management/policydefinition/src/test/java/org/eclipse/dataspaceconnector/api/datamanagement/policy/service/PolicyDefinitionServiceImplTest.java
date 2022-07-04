/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.policy.service;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.observe.policydefinition.PolicyDefinitionObservable;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.api.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PolicyDefinitionServiceImplTest {

    private final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);
    private final ContractDefinitionStore contractDefinitionStore = mock(ContractDefinitionStore.class);
    private final TransactionContext dummyTransactionContext = new NoopTransactionContext();

    private final PolicyDefinitionServiceImpl policyServiceImpl = new PolicyDefinitionServiceImpl(dummyTransactionContext, policyStore, contractDefinitionStore, mock(PolicyDefinitionObservable.class));

    @Test
    void findById_shouldRelyOnPolicyStore() {
        when(policyStore.findById("policyId")).thenReturn(createPolicy("policyId"));

        var policy = policyServiceImpl.findById("policyId");
        var uidTest = policy.getUid();

        String assetId = "policyId";
        assertThat(policy).isEqualTo(createPolicy("policyId"));
    }

    @Test
    void query_shouldRelyOnPolicyStore() {
        var policy = createPolicy("policyId");
        when(policyStore.findAll(any(QuerySpec.class))).thenReturn(Stream.of(policy));
        var policies = policyServiceImpl.query(QuerySpec.none());

        assertThat(policies).containsExactly(policy);
    }

    @Test
    void createPolicy_shouldCreatePolicyIfItDoesNotAlreadyExist() {
        var policy = createPolicy("policyId");
        when(policyStore.findById("policyId")).thenReturn(null);

        var inserted = policyServiceImpl.create(policy);

        assertThat(inserted.succeeded()).isTrue();
        assertThat(inserted.getContent()).isEqualTo(policy);
    }

    @Test
    void createPolicy_shouldNotCreatePolicyIfItAlreadyExists() {
        var policy = createPolicy("policyId");
        when(policyStore.findById("policyId")).thenReturn(policy);

        var inserted = policyServiceImpl.create(policy);

        assertThat(inserted.succeeded()).isFalse();
    }

    @Test
    void delete_shouldDeletePolicyIfItsNotReferencedByAnyContractDefinition() {
        when(contractDefinitionStore.findAll(any())).thenReturn(Stream.empty(), Stream.empty());
        when(policyStore.findById(any())).thenReturn(createPolicy("policyId"));
        when(policyStore.deleteById("policyId")).thenReturn(createPolicy("policyId"));

        var deleted = policyServiceImpl.deleteById("policyId");

        var result = deleted.getContent().getUid();

        assertThat(deleted.succeeded()).isTrue();
        assertThat(deleted.getContent()).matches(hasId("policyId"));
    }

    @Test
    void delete_shouldNotDeleteIfPolicyIsAlreadyPartOfContractDefinitionButPolicyDoesNotExistInPolicyStore() {
        var policy = createPolicy("policyId");
        when(policyStore.deleteById("policyId")).thenReturn(policy);

        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("A found Contract Definition")
                .accessPolicyId(policy.getUid())
                .contractPolicyId(policy.getUid())
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().constraint("left", "op", "right").build())
                .build();

        when(contractDefinitionStore.findAll(any())).thenReturn(Stream.of(contractDefinition));

        var deleted = policyServiceImpl.deleteById("policyId");

        assertThat(deleted.failed()).isTrue();
        assertThat(deleted.getFailure().getReason()).isEqualTo(NOT_FOUND);
    }

    @Test
    void delete_shouldNotDeleteIfPolicyIsAlreadyPartOfContractDefinitionButIsNotInContractDefinitionStore() {
        var policy = createPolicy("policyId");
        when(policyStore.deleteById("policyId")).thenReturn(policy);

        ContractDefinition contractDefinition = ContractDefinition.Builder.newInstance()
                .id("A found Contract Definition")
                .accessPolicyId(policy.getUid())
                .contractPolicyId(policy.getUid())
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().constraint("left", "op", "right").build())
                .build();

        when(contractDefinitionStore.findAll(any())).thenReturn(Stream.of(contractDefinition));

        var deleted = policyServiceImpl.deleteById("policyId");

        assertThat(deleted.failed()).isTrue();
        assertThat(deleted.getFailure().getReason()).isEqualTo(NOT_FOUND);
    }

    @Test
    void delete_shouldFailIfPolicyDoesNotExist() {
        when(policyStore.deleteById("policyId")).thenReturn(null);

        var deleted = policyServiceImpl.deleteById("policyId");

        assertThat(deleted.failed()).isTrue();
        assertThat(deleted.getFailure().getReason()).isEqualTo(NOT_FOUND);
    }

    @Test
    void delete_verifyCorrectQueries() {
        var policyId = "test-policy";
        when(policyStore.findById(policyId)).thenReturn(createPolicy(policyId));
        policyServiceImpl.deleteById(policyId);

        verify(contractDefinitionStore).findAll(argThat(qs -> qs.getFilterExpression().size() == 1 &&
                qs.getFilterExpression().get(0).getOperandLeft().equals("accessPolicyId")));
        verify(contractDefinitionStore).findAll(argThat(qs -> qs.getFilterExpression().size() == 1 &&
                qs.getFilterExpression().get(0).getOperandLeft().equals("contractPolicyId")));
    }


    @NotNull
    private Predicate<PolicyDefinition> hasId(String policyId) {
        return it -> policyId.equals(it.getUid());
    }

    private PolicyDefinition createPolicy(String policyId) {
        return PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).uid(policyId).build();
    }
}
