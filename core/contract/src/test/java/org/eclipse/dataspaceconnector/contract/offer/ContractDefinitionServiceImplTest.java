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

package org.eclipse.dataspaceconnector.contract.offer;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyStore;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression.SELECT_ALL;
import static org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService.NEGOTIATION_SCOPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractDefinitionServiceImplTest {

    private final PolicyEngine policyEngine = mock(PolicyEngine.class);
    private final PolicyStore policyStore = mock(PolicyStore.class);
    private final ContractDefinitionStore definitionStore = mock(ContractDefinitionStore.class);

    private ContractDefinitionServiceImpl definitionService;

    @BeforeEach
    void setUp() {
        definitionService = new ContractDefinitionServiceImpl(mock(Monitor.class), definitionStore, policyEngine, policyStore);
    }

    @Test
    void definitionsFor_verifySatisfiesPolicies() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        var policy = Policy.Builder.newInstance().build();
        when(policyStore.findById(any())).thenReturn(policy);
        when(policyEngine.evaluate(NEGOTIATION_SCOPE, policy, agent)).thenReturn(Result.success(policy));
        when(definitionStore.findAll()).thenReturn(List.of(ContractDefinition.Builder.newInstance().id("1").accessPolicyId("access").contractPolicyId("contract").selectorExpression(SELECT_ALL).build()));

        var definitions = definitionService.definitionsFor(agent);

        assertThat(definitions).hasSize(1);
        verify(policyEngine, atLeastOnce()).evaluate(NEGOTIATION_SCOPE, policy, agent);
        verify(definitionStore).findAll();
    }

    @Test
    void definitionsFor_verifyDoesNotSatisfyAccessPolicy() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        var policy = Policy.Builder.newInstance().id("access").build();
        when(policyStore.findById(any())).thenReturn(policy);
        var contractDefinition = ContractDefinition.Builder.newInstance().id("1").accessPolicyId("access").contractPolicyId("contract").selectorExpression(SELECT_ALL).build();
        when(policyEngine.evaluate(any(), any(), any())).thenReturn(Result.failure("invalid"));
        when(definitionStore.findAll()).thenReturn(List.of(contractDefinition));

        var result = definitionService.definitionsFor(agent);

        assertThat(result).isEmpty();
        verify(policyEngine, atLeastOnce()).evaluate(NEGOTIATION_SCOPE, policy, agent);
        verify(definitionStore).findAll();
    }

    @Test
    void definitionsFor_verifyDoesNotSatisfyUsagePolicy() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        var policy = Policy.Builder.newInstance().id("access").build();
        var contractDefinition = ContractDefinition.Builder.newInstance().id("1")
                .accessPolicyId("access").contractPolicyId("contract").selectorExpression(SELECT_ALL).build();
        when(policyStore.findById(any())).thenReturn(policy);
        when(policyEngine.evaluate(eq(NEGOTIATION_SCOPE), any(), any()))
                .thenReturn(Result.success(policy))
                .thenReturn(Result.failure("invalid"));
        when(definitionStore.findAll()).thenReturn(List.of(contractDefinition));

        var result = definitionService.definitionsFor(agent);

        assertThat(result).isEmpty();
        verify(policyEngine, atLeastOnce()).evaluate(NEGOTIATION_SCOPE, policy, agent);
        verify(definitionStore).findAll();
    }

    @Test
    void definitionsFor_verifyPoliciesNotFound() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        var policy = Policy.Builder.newInstance().build();
        when(policyStore.findById(any())).thenReturn(null);
        when(policyEngine.evaluate(NEGOTIATION_SCOPE, policy, agent)).thenReturn(Result.success(policy));
        when(definitionStore.findAll()).thenReturn(List.of(ContractDefinition.Builder.newInstance().id("1").accessPolicyId("access").contractPolicyId("contract").selectorExpression(SELECT_ALL).build()));

        var definitions = definitionService.definitionsFor(agent);

        assertThat(definitions).hasSize(0);
        verify(policyEngine, never()).evaluate(any(), any(), any());
    }

    @Test
    void definitionFor_found() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        var policy = Policy.Builder.newInstance().build();
        var contractDefinition = ContractDefinition.Builder.newInstance().id("1").accessPolicyId("access")
                .contractPolicyId("contract").selectorExpression(SELECT_ALL).build();
        when(policyStore.findById(any())).thenReturn(policy);
        when(policyEngine.evaluate(eq(NEGOTIATION_SCOPE), isA(Policy.class), isA(ParticipantAgent.class))).thenReturn(Result.success(policy));
        when(definitionStore.findAll()).thenReturn(List.of(contractDefinition));

        var result = definitionService.definitionFor(agent, "1");

        assertThat(result).isNotNull();
        verify(policyEngine, atLeastOnce()).evaluate(NEGOTIATION_SCOPE, policy, agent);
    }

    @Test
    void definitionFor_notFound() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        var contractDefinition = ContractDefinition.Builder.newInstance().id("1").accessPolicyId("access")
                .contractPolicyId("contract").selectorExpression(SELECT_ALL).build();
        when(definitionStore.findAll()).thenReturn(List.of(contractDefinition));

        var result = definitionService.definitionFor(agent, "nodefinition");

        assertThat(result).isNull();
        verify(policyEngine, never()).evaluate(any(), any(), any());
    }

}
