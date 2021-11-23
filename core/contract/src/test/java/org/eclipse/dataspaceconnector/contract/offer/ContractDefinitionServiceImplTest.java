package org.eclipse.dataspaceconnector.contract.offer;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.contract.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.contract.policy.PolicyResult;
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

    @Test
    void verifyCrudOperations() {
        var policy = Policy.Builder.newInstance().build();
        var definition1 = ContractDefinition.Builder.newInstance().id("1").accessPolicy(policy).contractPolicy(policy).selectorExpression(SELECT_ALL).build();
        var definition2 = ContractDefinition.Builder.newInstance().id("2").accessPolicy(policy).contractPolicy(policy).selectorExpression(SELECT_ALL).build();

        definitionService.load(List.of(definition1, definition2));

        assertThat(definitionService.getLoadedDefinitions().size()).isEqualTo(2);

        definitionService.remove(List.of("1"));
        assertThat(definitionService.getLoadedDefinitions()).contains(definition2);
    }

    @Test
    void verifySatisfiesPolicies() {
        var agent = new ParticipantAgent(Map.of(), Map.of());

        expect(policyEngine.evaluate(isA(Policy.class), isA(ParticipantAgent.class))).andReturn(new PolicyResult()).times(2);

        replay(policyEngine);

        var policy = Policy.Builder.newInstance().build();

        definitionService.load(List.of(ContractDefinition.Builder.newInstance().id("1").accessPolicy(policy).contractPolicy(policy).selectorExpression(SELECT_ALL).build()));

        assertThat(definitionService.definitionsFor(agent).count()).isEqualTo(1);

        verify(policyEngine);
    }

    @Test
    void verifyDoesNotSatisfyAccessPolicy() {
        var agent = new ParticipantAgent(Map.of(), Map.of());

        expect(policyEngine.evaluate(isA(Policy.class), isA(ParticipantAgent.class))).andReturn(new PolicyResult(List.of("invalid")));

        replay(policyEngine);

        var policy = Policy.Builder.newInstance().build();

        definitionService.load(List.of(ContractDefinition.Builder.newInstance().id("1").accessPolicy(policy).contractPolicy(policy).selectorExpression(SELECT_ALL).build()));

        assertThat(definitionService.definitionsFor(agent)).isEmpty();

        verify(policyEngine);
    }

    @Test
    void verifyDoesNotSatisfyUsagePolicy() {
        var agent = new ParticipantAgent(Map.of(), Map.of());

        expect(policyEngine.evaluate(isA(Policy.class), isA(ParticipantAgent.class))).andReturn(new PolicyResult()); // access policy valid
        expect(policyEngine.evaluate(isA(Policy.class), isA(ParticipantAgent.class))).andReturn(new PolicyResult(List.of("invalid"))); // usage policy invalid

        replay(policyEngine);

        var policy = Policy.Builder.newInstance().build();

        definitionService.load(List.of(ContractDefinition.Builder.newInstance().id("1").accessPolicy(policy).contractPolicy(policy).selectorExpression(SELECT_ALL).build()));

        assertThat(definitionService.definitionsFor(agent)).isEmpty();

        verify(policyEngine);
    }

    @Test
    void verifyDefinitionFor() {
        var agent = new ParticipantAgent(Map.of(), Map.of());

        expect(policyEngine.evaluate(isA(Policy.class), isA(ParticipantAgent.class))).andReturn(new PolicyResult()).times(2); // access and usage policy first check
        expect(policyEngine.evaluate(isA(Policy.class), isA(ParticipantAgent.class))).andReturn(new PolicyResult(List.of("invalid"))); // access policy second check

        replay(policyEngine);

        var policy = Policy.Builder.newInstance().build();

        definitionService.load(List.of(ContractDefinition.Builder.newInstance().id("1").accessPolicy(policy).contractPolicy(policy).selectorExpression(SELECT_ALL).build()));

        assertThat(definitionService.definitionFor(agent, "1").getDefinition()).isNotNull();
        assertThat(definitionService.definitionFor(agent, "1").invalid()).isTrue();
        assertThat(definitionService.definitionFor(agent, "nodefinition").invalid()).isTrue();

        verify(policyEngine);
    }

    @BeforeEach
    void setUp() {
        policyEngine = createMock(PolicyEngine.class);
        definitionService = new ContractDefinitionServiceImpl(policyEngine, createNiceMock(Monitor.class));
    }
}
