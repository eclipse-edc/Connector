/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.controlplane.catalog;

import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.edc.connector.controlplane.catalog.ContractDefinitionResolverImpl.POLICY_BATCH_SIZE;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContractDefinitionResolverImplTest {

    private final PolicyEngine policyEngine = mock();
    private final PolicyDefinitionStore policyStore = mock();
    private final ContractDefinitionStore definitionStore = mock();

    private final ParticipantContext participantContext = ParticipantContext.Builder.newInstance()
            .id("participantContextId")
            .identity("participantId")
            .build();

    private final ContractDefinitionResolverImpl resolver = new ContractDefinitionResolverImpl(definitionStore,
            policyEngine, policyStore);

    @Test
    void shouldReturnDefinition_whenAccessPolicySatisfied() {
        var agent = new ParticipantAgent("identity", emptyMap(), emptyMap());
        var def = policyDefinition("access");
        when(policyStore.findAll(any())).thenReturn(Stream.of(def));
        when(policyEngine.evaluate(any(), isA(PolicyContext.class))).thenReturn(Result.success());
        when(definitionStore.findAll(any())).thenReturn(Stream.of(createContractDefinition()));

        var result = resolver.resolveFor(participantContext, agent);

        assertThat(result.contractDefinitions()).hasSize(1);
        assertThat(result.policies()).hasSize(1);
        verify(policyEngine).evaluate(
                eq(def.getPolicy()),
                and(isA(CatalogPolicyContext.class), argThat(c -> c.participantAgent().equals(agent) && participantContext.getId().equals(c.participantContextId())))
        );
        var query = queryByParticipantContextId(participantContext.getId()).limit(Integer.MAX_VALUE).build();

        verify(definitionStore).findAll(query);
    }

    @Test
    void shouldNotReturnDefinition_whenAccessPolicyNotSatisfied() {
        var agent = new ParticipantAgent("identity", emptyMap(), emptyMap());
        when(policyStore.findAll(any())).thenReturn(Stream.of(policyDefinition("access")));
        var contractDefinition = createContractDefinition();
        when(policyEngine.evaluate(any(), isA(PolicyContext.class))).thenReturn(Result.failure("invalid"));
        when(definitionStore.findAll(any())).thenReturn(Stream.of(contractDefinition));

        var result = resolver.resolveFor(participantContext, agent);

        assertThat(result.contractDefinitions()).isEmpty();
        assertThat(result.policies()).hasSize(1);
        verify(definitionStore).findAll(any());
    }

    @Test
    void shouldNotReturnDefinition_whenAccessPolicyDoesNotExist() {
        var agent = new ParticipantAgent("identity", emptyMap(), emptyMap());
        when(policyStore.findAll(any())).thenReturn(Stream.empty());
        when(policyEngine.evaluate(any(), isA(PolicyContext.class))).thenReturn(Result.success());
        when(definitionStore.findAll(any())).thenReturn(Stream.of(createContractDefinition()));

        var result = resolver.resolveFor(participantContext, agent);

        assertThat(result.contractDefinitions()).isEmpty();
        assertThat(result.policies()).isEmpty();
        verifyNoInteractions(policyEngine);
    }

    @Test
    void shouldLoadAndEvaluatePolicyOnce_whenDifferentDefinitionsHaveSamePolicy() {
        var contractDefinition1 = contractDefinitionBuilder().id("1").accessPolicyId("accessPolicyId").build();
        var contractDefinition2 = contractDefinitionBuilder().id("2").accessPolicyId("accessPolicyId").build();
        var policyDefinition = policyDefinition("accessPolicyId");
        when(policyStore.findAll(any())).thenReturn(Stream.of(policyDefinition));
        when(policyEngine.evaluate(any(), isA(PolicyContext.class))).thenReturn(Result.success());
        when(definitionStore.findAll(any())).thenReturn(Stream.of(contractDefinition1, contractDefinition2));

        var result = resolver.resolveFor(participantContext, new ParticipantAgent("identity", emptyMap(), emptyMap()));

        assertThat(result.contractDefinitions()).containsExactly(contractDefinition1, contractDefinition2);
        assertThat(result.policies()).hasSize(1).containsOnly(entry("accessPolicyId", policyDefinition.getPolicy()));
        verify(policyStore).findAll(any());
        verify(policyStore, never()).findById(any(), any());
        verify(policyEngine, times(1)).evaluate(any(), any());
    }

    @Test
    void shouldLoadPoliciesInBatches() {
        var policyCount = POLICY_BATCH_SIZE + 1;
        var definitions = IntStream.range(0, policyCount)
                .mapToObj(i -> contractDefinitionBuilder().id("cd-" + i).accessPolicyId("policy-" + i).build())
                .toList();
        when(definitionStore.findAll(any())).thenReturn(definitions.stream());
        when(policyStore.findAll(any())).thenAnswer(invocation -> requestedIds(invocation.getArgument(0)).stream().map(this::policyDefinition));
        when(policyEngine.evaluate(any(), isA(PolicyContext.class))).thenReturn(Result.success());

        var result = resolver.resolveFor(participantContext, new ParticipantAgent("identity", emptyMap(), emptyMap()));

        assertThat(result.contractDefinitions()).containsExactlyElementsOf(definitions);
        assertThat(result.policies()).hasSize(policyCount);
        var queries = ArgumentCaptor.forClass(QuerySpec.class);
        verify(policyStore, times(2)).findAll(queries.capture());
        assertThat(queries.getAllValues()).allSatisfy(query -> {
            assertThat(query.getFilterExpression()).contains(new Criterion("participantContextId", "=", participantContext.getId()));
            assertThat(query.getLimit()).isEqualTo(requestedIds(query).size());
        });
        assertThat(queries.getAllValues()).extracting(query -> requestedIds(query).size()).containsExactly(POLICY_BATCH_SIZE, 1);
    }

    @Test
    void shouldNotQueryPolicies_whenNoDefinitions() {
        when(definitionStore.findAll(any())).thenReturn(Stream.empty());

        var result = resolver.resolveFor(participantContext, new ParticipantAgent("identity", emptyMap(), emptyMap()));

        assertThat(result.contractDefinitions()).isEmpty();
        verifyNoInteractions(policyStore, policyEngine);
    }

    @SuppressWarnings("unchecked")
    private Collection<String> requestedIds(QuerySpec query) {
        return query.getFilterExpression().stream()
                .filter(criterion -> "id".equals(criterion.getOperandLeft()) && "in".equals(criterion.getOperator()))
                .map(criterion -> (Collection<String>) criterion.getOperandRight())
                .findFirst()
                .orElse(List.of());
    }

    private PolicyDefinition policyDefinition(String id) {
        return PolicyDefinition.Builder.newInstance().id(id).policy(Policy.Builder.newInstance().build()).build();
    }

    private ContractDefinition createContractDefinition() {
        return contractDefinitionBuilder()
                .build();
    }

    private ContractDefinition.Builder contractDefinitionBuilder() {
        return ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicyId("access")
                .contractPolicyId("contract");
    }
}
