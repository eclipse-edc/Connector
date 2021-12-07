package org.eclipse.dataspaceconnector.contract.offer;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.contract.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression.SELECT_ALL;

class ContractDefinitionServiceImplTest {
    private ContractDefinitionServiceImpl definitionService;
    private PolicyEngine policyEngine;
    private ContractDefinitionStore definitionStore;

    @Test
    void verifySatisfiesPolicies() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        var policy = Policy.Builder.newInstance().build();

        expect(policyEngine.evaluate(isA(Policy.class), isA(ParticipantAgent.class))).andReturn(Result.success(policy)).times(2);
        expect(definitionStore.findAll()).andReturn(List.of(ContractDefinition.Builder.newInstance().id("1").accessPolicy(policy).contractPolicy(policy).selectorExpression(SELECT_ALL).build()));

        replay(definitionStore, policyEngine);

        assertThat(definitionService.definitionsFor(agent).count()).isEqualTo(1);

        verify(definitionStore, policyEngine);
    }

    @Test
    void verifyDoesNotSatisfyAccessPolicy() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        var policy = Policy.Builder.newInstance().build();

        expect(policyEngine.evaluate(isA(Policy.class), isA(ParticipantAgent.class))).andReturn(Result.failure("invalid"));
        expect(definitionStore.findAll()).andReturn(List.of(ContractDefinition.Builder.newInstance().id("1").accessPolicy(policy).contractPolicy(policy).selectorExpression(SELECT_ALL).build()));

        replay(definitionStore, policyEngine);

        assertThat(definitionService.definitionsFor(agent)).isEmpty();

        verify(definitionStore, policyEngine);
    }

    @Test
    void verifyDoesNotSatisfyUsagePolicy() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        var policy = Policy.Builder.newInstance().build();

        expect(policyEngine.evaluate(isA(Policy.class), isA(ParticipantAgent.class))).andReturn(Result.success(policy)); // access policy valid
        expect(policyEngine.evaluate(isA(Policy.class), isA(ParticipantAgent.class))).andReturn(Result.failure("invalid")); // usage policy invalid
        expect(definitionStore.findAll()).andReturn(List.of(ContractDefinition.Builder.newInstance().id("1").accessPolicy(policy).contractPolicy(policy).selectorExpression(SELECT_ALL).build()));

        replay(definitionStore, policyEngine);

        assertThat(definitionService.definitionsFor(agent)).isEmpty();

        verify(definitionStore, policyEngine);
    }

    @Test
    void verifyDefinitionFor() {
        var agent = new ParticipantAgent(Map.of(), Map.of());
        var policy = Policy.Builder.newInstance().build();

        expect(policyEngine.evaluate(isA(Policy.class), isA(ParticipantAgent.class))).andReturn(Result.success(policy)).times(2); // access and usage policy first check
        expect(policyEngine.evaluate(isA(Policy.class), isA(ParticipantAgent.class))).andReturn(Result.failure("invalid")); // access policy second check
        expect(definitionStore.findAll()).andReturn(List.of(ContractDefinition.Builder.newInstance().id("1").accessPolicy(policy).contractPolicy(policy).selectorExpression(SELECT_ALL).build())).anyTimes();

        replay(definitionStore, policyEngine);

        assertThat(definitionService.definitionFor(agent, "1")).isNotNull();
        assertThat(definitionService.definitionFor(agent, "1")).isNull();
        assertThat(definitionService.definitionFor(agent, "nodefinition")).isNull();

        verify(definitionStore, policyEngine);
    }

    @BeforeEach
    void setUp() {
        policyEngine = createMock(PolicyEngine.class);
        definitionStore = createMock(ContractDefinitionStore.class);
        definitionService = new ContractDefinitionServiceImpl(createNiceMock(Monitor.class), definitionStore, policyEngine);
    }
}
