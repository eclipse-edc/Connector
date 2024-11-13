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

package org.eclipse.edc.connector.controlplane.services.policydefinition;

import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.observe.PolicyDefinitionObservable;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.controlplane.services.query.QueryValidator;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class PolicyDefinitionServiceImplTest {

    private final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);
    private final ContractDefinitionStore contractDefinitionStore = mock(ContractDefinitionStore.class);
    private final TransactionContext dummyTransactionContext = new NoopTransactionContext();
    private final PolicyDefinitionObservable observable = mock(PolicyDefinitionObservable.class);
    private final PolicyEngine policyEngine = mock();
    private final QueryValidator queryValidator = mock();
    private final PolicyDefinitionServiceImpl policyServiceImpl = new PolicyDefinitionServiceImpl(dummyTransactionContext, policyStore, contractDefinitionStore, observable, policyEngine, queryValidator, false);

    @Test
    void findById_shouldRelyOnPolicyStore() {
        when(policyStore.findById("policyId")).thenReturn(createPolicy("policyId"));

        var policy = policyServiceImpl.findById("policyId");

        assertThat(policy).isEqualTo(createPolicy("policyId"));
    }

    @Test
    void search_shouldRelyOnPolicyStore() {
        var policy = createPolicy("policyId");
        when(policyStore.findAll(any(QuerySpec.class))).thenReturn(Stream.of(policy));
        when(queryValidator.validate(any())).thenReturn(Result.success());

        var policies = policyServiceImpl.search(QuerySpec.none());

        assertThat(policies).isSucceeded().asInstanceOf(list(PolicyDefinition.class)).containsExactly(policy);
    }

    @Test
    void search_shouldFail_whenValidationFails() {
        when(queryValidator.validate(any())).thenReturn(Result.failure("not valid"));

        var policies = policyServiceImpl.search(QuerySpec.none());

        assertThat(policies).isFailed();
        verifyNoInteractions(policyStore);
    }

    @Test
    void createPolicy_shouldCreatePolicyIfItDoesNotAlreadyExist() {
        var policy = createPolicy("policyId");
        when(policyStore.create(policy)).thenReturn(StoreResult.success(policy));

        var inserted = policyServiceImpl.create(policy);

        assertThat(inserted.succeeded()).isTrue();
        assertThat(inserted.getContent()).isEqualTo(policy);
        verify(policyStore).create(policy);
        verifyNoMoreInteractions(policyStore);
    }

    @Test
    void createPolicy_shouldNotCreatePolicyIfItAlreadyExists() {
        var policy = createPolicy("policyId");
        when(policyStore.create(policy)).thenReturn(StoreResult.alreadyExists("test"));

        var inserted = policyServiceImpl.create(policy);

        assertThat(inserted.succeeded()).isFalse();
        verify(policyStore).create(policy);
        verifyNoMoreInteractions(policyStore);
    }

    @Test
    void createPolicy_shouldCreatePolicyIfValidationSucceed() {

        var policyServiceImpl = new PolicyDefinitionServiceImpl(dummyTransactionContext, policyStore, contractDefinitionStore, observable, policyEngine, queryValidator, true);

        var policy = createPolicy("policyId");
        when(policyStore.create(policy)).thenReturn(StoreResult.success(policy));
        when(policyEngine.validate(policy.getPolicy())).thenReturn(Result.success());

        var inserted = policyServiceImpl.create(policy);

        assertThat(inserted).isSucceeded();
        verify(policyStore).create(policy);
        verifyNoMoreInteractions(policyStore);
    }

    @Test
    void createPolicy_shouldNotCreatePolicyIfValidationFails() {

        var policyServiceImpl = new PolicyDefinitionServiceImpl(dummyTransactionContext, policyStore, contractDefinitionStore, observable, policyEngine, queryValidator, true);

        var policy = createPolicy("policyId");
        when(policyStore.create(policy)).thenReturn(StoreResult.success(policy));
        when(policyEngine.validate(policy.getPolicy())).thenReturn(Result.failure("validation failure"));

        var inserted = policyServiceImpl.create(policy);

        assertThat(inserted).isFailed().messages().contains("validation failure");
        verifyNoMoreInteractions(policyStore);
    }

    @Test
    void delete_shouldDeletePolicyIfItsNotReferencedByAnyContractDefinition() {
        when(contractDefinitionStore.findAll(any())).thenReturn(Stream.empty(), Stream.empty());
        when(policyStore.findById(any())).thenReturn(createPolicy("policyId"));
        when(policyStore.delete("policyId")).thenReturn(StoreResult.success(createPolicy("policyId")));

        var deleted = policyServiceImpl.deleteById("policyId");

        assertThat(deleted.succeeded()).isTrue();
        assertThat(deleted.getContent()).matches(hasId("policyId"));
    }

    @Test
    void delete_shouldNotDelete_whenPolicyPartOfContractDef() {
        var policy = createPolicy("policyId");
        when(policyStore.delete("policyId")).thenReturn(StoreResult.success(policy));

        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("A found Contract Definition")
                .accessPolicyId(policy.getId())
                .contractPolicyId(policy.getId())
                .assetsSelectorCriterion(criterion("left", "op", "right"))
                .build();

        when(contractDefinitionStore.findAll(any())).thenReturn(Stream.of(contractDefinition));

        var deleted = policyServiceImpl.deleteById("policyId");

        assertThat(deleted.failed()).isTrue();
        assertThat(deleted.getFailure().getReason()).isEqualTo(CONFLICT);
    }

    @Test
    void delete_shouldNotDelete_whenPolicyIsPartOfContractDefinition() {
        var policy = createPolicy("policyId");
        when(policyStore.delete("policyId")).thenReturn(StoreResult.success(policy));

        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("A found Contract Definition")
                .accessPolicyId(policy.getId())
                .contractPolicyId(policy.getId())
                .assetsSelectorCriterion(criterion("left", "op", "right"))
                .build();

        when(contractDefinitionStore.findAll(any())).thenReturn(Stream.of(contractDefinition));

        var deleted = policyServiceImpl.deleteById("policyId");

        assertThat(deleted.failed()).isTrue();
        assertThat(deleted.getFailure().getReason()).isEqualTo(CONFLICT);
    }

    @Test
    void delete_shouldFailIfPolicyDoesNotExist() {
        when(policyStore.delete("policyId")).thenReturn(StoreResult.notFound("test"));

        var deleted = policyServiceImpl.deleteById("policyId");

        assertThat(deleted.failed()).isTrue();
        assertThat(deleted.getFailure().getReason()).isEqualTo(NOT_FOUND);
    }

    @Test
    void delete_verifyCorrectQueries() {
        var policyId = "test-policy";
        when(policyStore.delete(policyId)).thenReturn(StoreResult.success());
        policyServiceImpl.deleteById(policyId);

        verify(policyStore).delete(eq(policyId));
        verifyNoMoreInteractions(policyStore);
    }

    @Test
    void updatePolicy_ifPolicyNotExists() {
        var policy = createPolicy("policyId");
        when(policyStore.update(policy)).thenReturn(StoreResult.notFound("test"));
        var updated = policyServiceImpl.update(policy);
        assertThat(updated.succeeded()).isFalse();
        assertThat(updated.getContent()).isNull();
        verify(policyStore).update(any());
        verifyNoMoreInteractions(policyStore);

    }

    @Test
    void updatePolicy_shouldUpdateWhenExists() {
        var policyId = "policyId";
        var policy = createPolicy(policyId);
        when(policyStore.update(policy)).thenReturn(StoreResult.success(policy));

        var updated = policyServiceImpl.update(policy);

        assertThat(updated.succeeded()).isTrue();
        verify(policyStore).update(eq(policy));
        verifyNoMoreInteractions(policyStore);
        verify(observable).invokeForEach(any());
    }

    @Test
    void updatePolicy_shouldReturnNotFound_whenNotExists() {
        var policyId = "policyId";
        var policy = createPolicy(policyId);
        when(policyStore.update(policy)).thenReturn(StoreResult.notFound("test"));

        var updated = policyServiceImpl.update(policy);

        assertThat(updated.failed()).isTrue();
        assertThat(updated.reason()).isEqualTo(NOT_FOUND);
        verify(policyStore).update(policy);
        verifyNoMoreInteractions(policyStore);
        verify(observable, never()).invokeForEach(any());
    }

    @Test
    void validatePolicy() {
        var policyId = "policyId";
        var policy = createPolicy(policyId);

        when(policyEngine.validate(policy.getPolicy())).thenReturn(Result.success());

        assertThat(policyServiceImpl.validate(policy.getPolicy())).isSucceeded();
    }

    @Test
    void validatePolicy_shouldFail_whenPolicyEngineFails() {
        var policyId = "policyId";
        var policy = createPolicy(policyId);

        when(policyEngine.validate(policy.getPolicy())).thenReturn(Result.failure("validation failure"));

        assertThat(policyServiceImpl.validate(policy.getPolicy())).isFailed()
                .detail()
                .isEqualTo("validation failure");
    }

    @NotNull
    private Predicate<PolicyDefinition> hasId(String policyId) {
        return it -> policyId.equals(it.getId());
    }

    private PolicyDefinition createPolicy(String policyId) {
        return PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).id(policyId).build();
    }

}
