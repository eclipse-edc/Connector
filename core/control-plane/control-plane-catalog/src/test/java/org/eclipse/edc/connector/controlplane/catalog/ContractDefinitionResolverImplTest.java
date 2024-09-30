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

import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.edc.connector.controlplane.catalog.CatalogCoreExtension.CATALOG_SCOPE;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContractDefinitionResolverImplTest {

    private final PolicyEngine policyEngine = mock();
    private final PolicyDefinitionStore policyStore = mock();
    private final ContractDefinitionStore definitionStore = mock();

    private final ContractDefinitionResolverImpl resolver = new ContractDefinitionResolverImpl(definitionStore,
            policyEngine, policyStore);

    @Test
    void shouldReturnDefinition_whenAccessPolicySatisfied() {
        var agent = new ParticipantAgent(emptyMap(), emptyMap());
        var def = PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();
        when(policyStore.findById(any())).thenReturn(def);
        when(policyEngine.evaluate(any(), any(), isA(PolicyContext.class))).thenReturn(Result.success());
        when(definitionStore.findAll(any())).thenReturn(Stream.of(createContractDefinition()));

        var result = resolver.resolveFor(agent);

        assertThat(result.contractDefinitions()).hasSize(1);
        assertThat(result.policies()).hasSize(1);
        verify(policyEngine, atLeastOnce()).evaluate(
                eq(CATALOG_SCOPE),
                eq(def.getPolicy()),
                and(isA(PolicyContext.class), argThat(c -> c.getContextData(ParticipantAgent.class).equals(agent)))
        );
        verify(definitionStore).findAll(any());
    }

    @Test
    void shouldNotReturnDefinition_whenAccessPolicyNotSatisfied() {
        var agent = new ParticipantAgent(emptyMap(), emptyMap());
        var definition = PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).id("access").build();
        when(policyStore.findById(any())).thenReturn(definition);
        var contractDefinition = createContractDefinition();
        when(policyEngine.evaluate(any(), any(), isA(PolicyContext.class))).thenReturn(Result.failure("invalid"));
        when(definitionStore.findAll(any())).thenReturn(Stream.of(contractDefinition));

        var result = resolver.resolveFor(agent);

        assertThat(result.contractDefinitions()).isEmpty();
        assertThat(result.policies()).hasSize(1);
        verify(definitionStore).findAll(any());
    }

    @Test
    void shouldNotReturnDefinition_whenAccessPolicyDoesNotExist() {
        var agent = new ParticipantAgent(emptyMap(), emptyMap());
        when(policyStore.findById(any())).thenReturn(null);
        when(policyEngine.evaluate(any(), any(), isA(PolicyContext.class))).thenReturn(Result.success());
        when(definitionStore.findAll(QuerySpec.max())).thenReturn(Stream.of(createContractDefinition()));

        var result = resolver.resolveFor(agent);

        assertThat(result.contractDefinitions()).isEmpty();
        assertThat(result.policies()).isEmpty();
        verifyNoInteractions(policyEngine);
    }

    @Test
    void shouldQueryPolicyOnce_whenDifferentDefinitionsHaveSamePolicy() {
        var contractDefinition1 = contractDefinitionBuilder().accessPolicyId("accessPolicyId").build();
        var contractDefinition2 = contractDefinitionBuilder().accessPolicyId("accessPolicyId").build();
        var policy = Policy.Builder.newInstance().build();
        var policyDefinition = PolicyDefinition.Builder.newInstance().policy(policy).build();
        when(policyStore.findById(any())).thenReturn(policyDefinition);
        when(policyEngine.evaluate(any(), any(), isA(PolicyContext.class))).thenReturn(Result.success());
        when(definitionStore.findAll(any())).thenReturn(Stream.of(contractDefinition1, contractDefinition2));

        var result = resolver.resolveFor(new ParticipantAgent(emptyMap(), emptyMap()));

        assertThat(result.contractDefinitions()).hasSize(2);
        assertThat(result.policies()).hasSize(1).containsOnly(entry("accessPolicyId", policy));
        verify(policyStore, only()).findById("accessPolicyId");
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
