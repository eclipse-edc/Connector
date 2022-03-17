package org.eclipse.dataspaceconnector.contract.offer;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression.SELECT_ALL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractDefinitionServiceImplTest {
    private final PolicyEngine policyEngine = mock(PolicyEngine.class);
    private final ContractDefinitionStore definitionStore = mock(ContractDefinitionStore.class);

    private ContractDefinitionServiceImpl definitionService;

    @BeforeEach
    void setUp() {
        definitionService = new ContractDefinitionServiceImpl(mock(Monitor.class), definitionStore, policyEngine);
    }

    @Test
    void verifySatisfiesPolicies() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        var policy = Policy.Builder.newInstance().build();

        when(policyEngine.evaluate(policy, agent)).thenReturn(Result.success(policy));
        when(definitionStore.findAll()).thenReturn(List.of(ContractDefinition.Builder.newInstance().id("1").accessPolicy(policy).contractPolicy(policy).selectorExpression(SELECT_ALL).build()));

        var definitions = definitionService.definitionsFor(agent);

        assertThat(definitions).hasSize(1);
        verify(policyEngine, atLeastOnce()).evaluate(policy, agent);
        verify(definitionStore).findAll();
    }

    @Test
    void verifyDoesNotSatisfyAccessPolicy() {
        var agent = new ParticipantAgent(Map.of(), Map.of());

        var policy = Policy.Builder.newInstance().id("access").build();
        var contractDefinition = ContractDefinition.Builder.newInstance().id("1").accessPolicy(policy).contractPolicy(policy).selectorExpression(SELECT_ALL).build();

        when(policyEngine.evaluate(any(), any()))
                .thenReturn(Result.failure("invalid"));
        when(definitionStore.findAll()).thenReturn(List.of(contractDefinition));

        assertThat(definitionService.definitionsFor(agent)).isEmpty();
        verify(policyEngine, atLeastOnce()).evaluate(policy, agent);
        verify(definitionStore).findAll();
    }

    @Test
    void verifyDoesNotSatisfyUsagePolicy() {
        var agent = new ParticipantAgent(Map.of(), Map.of());

        var policy = Policy.Builder.newInstance().id("access").build();
        var contractDefinition = ContractDefinition.Builder.newInstance().id("1")
                .accessPolicy(policy).contractPolicy(policy).selectorExpression(SELECT_ALL).build();
        when(policyEngine.evaluate(any(), any()))
                .thenReturn(Result.success(policy))
                .thenReturn(Result.failure("invalid"));
        when(definitionStore.findAll()).thenReturn(List.of(contractDefinition));

        assertThat(definitionService.definitionsFor(agent)).isEmpty();
        verify(policyEngine, atLeastOnce()).evaluate(policy, agent);
        verify(definitionStore).findAll();
    }

    @Test
    void verifyDefinitionFor() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        var policy = Policy.Builder.newInstance().build();
        var contractDefinition = ContractDefinition.Builder.newInstance().id("1").accessPolicy(policy)
                .contractPolicy(policy).selectorExpression(SELECT_ALL).build();
        when(policyEngine.evaluate(isA(Policy.class), isA(ParticipantAgent.class))).thenReturn(Result.success(policy));
        when(definitionStore.findAll()).thenReturn(List.of(contractDefinition));

        assertThat(definitionService.definitionFor(agent, "1")).isNotNull();
        assertThat(definitionService.definitionFor(agent, "nodefinition")).isNull();
        verify(policyEngine, atLeastOnce()).evaluate(policy, agent);
        verify(definitionStore, times(2)).findAll();
    }
}
