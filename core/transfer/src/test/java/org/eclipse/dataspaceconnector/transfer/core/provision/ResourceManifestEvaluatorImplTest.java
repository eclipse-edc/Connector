/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.core.provision;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.core.base.policy.PolicyContextImpl;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.policy.model.Rule;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.policy.model.Operator.EQ;
import static org.mockito.Mockito.mock;

class ResourceManifestEvaluatorImplTest {
    
    private ResourceManifestEvaluatorImpl evaluator;
    
    @BeforeEach
    void init() {
        evaluator = new ResourceManifestEvaluatorImpl(mock(Monitor.class));
    }
    
    @ParameterizedTest
    @ValueSource(classes = {Permission.class, Prohibition.class, Duty.class})
    void verifyResourceDefinitionRuleFunction(Class<? extends Rule> ruleType) {
        var definition = TestDefinition.Builder.newInstance().id("id").build();
        var manifest = ResourceManifest.Builder.newInstance().definitions(List.of(definition)).build();
        var policy = getPolicyForRuleFunction(ruleType);
    
        var newId = definition.getId().concat("-modified");
        evaluator.registerFunction(TestDefinition.class, ruleType, (rule, def) -> Result.success(modifyDefinition(newId)));
        
        var policyContext = new PolicyContextImpl(new ParticipantAgent(emptyMap(), emptyMap()));
        policyContext.putContextData(ResourceManifest.class, manifest);
        
        var result = evaluator.evaluate(policy, policyContext);
        
        assertThat(result).isTrue();
        assertThat(policyContext.getProblems()).isEmpty();
        
        var modifiedManifest = policyContext.getContextData(ResourceManifest.class);
        assertThat(modifiedManifest).isNotNull();
        assertThat(modifiedManifest.getDefinitions()).isNotEmpty().hasSize(1);
        
        var modifiedDefinition = (TestDefinition) modifiedManifest.getDefinitions().get(0);
        assertThat(modifiedDefinition).isNotEqualTo(definition);
        assertThat(modifiedDefinition.getId()).isEqualTo(newId);
    }
    
    @ParameterizedTest
    @ValueSource(classes = {Permission.class, Prohibition.class, Duty.class})
    void verifyResourceDefinitionRuleFunction_policyNotFulfilled(Class<? extends Rule> ruleType) {
        var errorMessage = "error";
        
        var definition = TestDefinition.Builder.newInstance().id("id").build();
        var manifest = ResourceManifest.Builder.newInstance().definitions(List.of(definition)).build();
        var policy = getPolicyForRuleFunction(ruleType);
    
        evaluator.registerFunction(TestDefinition.class, ruleType, (rule, def) -> Result.failure(errorMessage));
        
        var policyContext = new PolicyContextImpl(new ParticipantAgent(emptyMap(), emptyMap()));
        policyContext.putContextData(ResourceManifest.class, manifest);
        
        var result = evaluator.evaluate(policy, policyContext);
        
        assertThat(result).isFalse();
        assertThat(policyContext.getProblems()).hasSize(1).containsExactly(errorMessage);
    }
    
    @ParameterizedTest
    @ValueSource(classes = {Permission.class, Prohibition.class, Duty.class})
    void verifyResourceDefinitionConstraintFunction(Class<? extends Rule> ruleType) {
        var key = "foo";
    
        var definition = TestDefinition.Builder.newInstance().id("id").build();
        var manifest = ResourceManifest.Builder.newInstance().definitions(List.of(definition)).build();
        var policy = getPolicyForConstraintFunction(ruleType);
    
        var newId = definition.getId().concat("-modified");
        evaluator.registerFunction(key, TestDefinition.class, ruleType, (op, rv, rule, def) -> Result.success(modifyDefinition(newId)));
        
        var policyContext = new PolicyContextImpl(new ParticipantAgent(emptyMap(), emptyMap()));
        policyContext.putContextData(ResourceManifest.class, manifest);
        
        var result = evaluator.evaluate(policy, policyContext);
        
        assertThat(result).isTrue();
        assertThat(policyContext.getProblems()).isEmpty();
    
        var modifiedManifest = policyContext.getContextData(ResourceManifest.class);
        assertThat(modifiedManifest).isNotNull();
        assertThat(modifiedManifest.getDefinitions()).isNotEmpty().hasSize(1);
    
        var modifiedDefinition = (TestDefinition) modifiedManifest.getDefinitions().get(0);
        assertThat(modifiedDefinition).isNotEqualTo(definition);
        assertThat(modifiedDefinition.getId()).isEqualTo(newId);
    }
    
    @ParameterizedTest
    @ValueSource(classes = {Permission.class, Prohibition.class, Duty.class})
    void verifyResourceDefinitionConstraintFunction_policyNotFulfilled(Class<? extends Rule> ruleType) {
        var key = "foo";
        var errorMessage = "error";
        
        var definition = TestDefinition.Builder.newInstance().id("id").build();
        var manifest = ResourceManifest.Builder.newInstance().definitions(List.of(definition)).build();
        var policy = getPolicyForConstraintFunction(ruleType);
    
        evaluator.registerFunction(key, TestDefinition.class, ruleType, (op, rv, rule, def) -> Result.failure(errorMessage));
    
        var policyContext = new PolicyContextImpl(new ParticipantAgent(emptyMap(), emptyMap()));
        policyContext.putContextData(ResourceManifest.class, manifest);
        
        var result = evaluator.evaluate(policy, policyContext);
    
        assertThat(result).isFalse();
        assertThat(policyContext.getProblems()).hasSize(1).containsExactly(errorMessage);
    }
    
    private Policy getPolicyForRuleFunction(Class<? extends Rule> ruleType) {
        var action = Action.Builder.newInstance().type("USE").build();
        var policyBuilder = Policy.Builder.newInstance();
        
        if (Permission.class.isAssignableFrom(ruleType)) {
            var permission = Permission.Builder.newInstance().action(action).build();
            policyBuilder.permission(permission);
        } else if (Prohibition.class.isAssignableFrom(ruleType)) {
            var prohibition = Prohibition.Builder.newInstance().action(action).build();
            policyBuilder.prohibition(prohibition);
        } else if (Duty.class.isAssignableFrom(ruleType)) {
            var duty = Duty.Builder.newInstance().action(action).build();
            policyBuilder.duty(duty);
        }
        
        return policyBuilder.build();
    }
    
    private Policy getPolicyForConstraintFunction(Class<? extends Rule> ruleType) {
        var left = new LiteralExpression("foo");
        var right = new LiteralExpression("bar");
        var action = Action.Builder.newInstance().type("USE").build();
        var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();
        var policyBuilder = Policy.Builder.newInstance();
        
        if (Permission.class.isAssignableFrom(ruleType)) {
            var permission = Permission.Builder.newInstance().action(action).constraint(constraint).build();
            policyBuilder.permission(permission);
        } else if (Prohibition.class.isAssignableFrom(ruleType)) {
            var prohibition = Prohibition.Builder.newInstance().action(action).constraint(constraint).build();
            policyBuilder.prohibition(prohibition);
        } else if (Duty.class.isAssignableFrom(ruleType)) {
            var duty = Duty.Builder.newInstance().action(action).constraint(constraint).build();
            policyBuilder.duty(duty);
        }
    
        return policyBuilder.build();
    }
    
    private TestDefinition modifyDefinition(String newId) {
        return TestDefinition.Builder.newInstance().id(newId).build();
    }
    
    public static class TestDefinition extends ResourceDefinition {
        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder extends ResourceDefinition.Builder<TestDefinition, TestDefinition.Builder> {
            protected Builder() {
                super(new TestDefinition());
            }
            
            @JsonCreator
            public static TestDefinition.Builder newInstance() {
                return new TestDefinition.Builder();
            }
        }
    }
    
}
