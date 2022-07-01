/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - resource manifest evaluation
 *
 */

package org.eclipse.dataspaceconnector.policy.engine.model;

import org.eclipse.dataspaceconnector.policy.engine.PolicyEvaluator;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.policy.model.Rule;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.policy.engine.model.PolicyTestFunctions.RuleTypeArguments;
import static org.eclipse.dataspaceconnector.policy.engine.model.PolicyTestFunctions.TestDefinition;
import static org.eclipse.dataspaceconnector.policy.engine.model.PolicyTestFunctions.createLiteralAtomicConstraint;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyEvaluatorTest {

    @Test
    void verifySimpleEval() {
        var constraint = createLiteralAtomicConstraint("foo", "foo");

        var duty = Duty.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().duty(duty).build();

        var evaluator = PolicyEvaluator.Builder.newInstance().build();
        assertTrue(evaluator.evaluate(policy).valid());
    }

    @Test
    void verifyProhibitionNotEqualEval() {
        var constraint = createLiteralAtomicConstraint("baz", "bar");

        var prohibition = Prohibition.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().prohibition(prohibition).build();

        var evaluator = PolicyEvaluator.Builder.newInstance().build();
        assertTrue(evaluator.evaluate(policy).valid());
    }

    @Test
    void verifyPermissionNotEqualEval() {
        var constraint = createLiteralAtomicConstraint("baz", "bar");

        var permission = Permission.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().permission(permission).build();

        var evaluator = PolicyEvaluator.Builder.newInstance().build();
        assertFalse(evaluator.evaluate(policy).valid());
    }

    @Test
    void verifyPermissionFunctions() {
        var constraint = createLiteralAtomicConstraint("toResolve", "foo");

        var permission = Permission.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().permission(permission).build();

        var evaluator = PolicyEvaluator.Builder.newInstance().permissionFunction("toResolve", (operator, value, p) -> "foo".equals(value)).build();
        assertTrue(evaluator.evaluate(policy).valid());
    }

    @Test
    void verifyDutyFunctions() {
        var constraint = createLiteralAtomicConstraint("toResolve", "foo");

        var duty = Duty.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().duty(duty).build();

        var evaluator = PolicyEvaluator.Builder.newInstance().dutyFunction("toResolve", (operator, value, d) -> "foo".equals(value)).build();
        assertTrue(evaluator.evaluate(policy).valid());
    }

    @Test
    void verifyProhibitionFunctions() {
        var constraint = createLiteralAtomicConstraint("toResolve", "foo");

        var prohibition = Prohibition.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().prohibition(prohibition).build();

        var evaluator = PolicyEvaluator.Builder.newInstance().prohibitionFunction("toResolve", (operator, value, pr) -> !"foo".equals(value)).build();
        assertTrue(evaluator.evaluate(policy).valid());
    }

    @Test
    void verifyPermissionRuleFunctions() {
        var permission = Permission.Builder.newInstance().action(Action.Builder.newInstance().type("USE").build()).build();
        var policy = Policy.Builder.newInstance().permission(permission).build();

        var evaluator = PolicyEvaluator.Builder.newInstance().permissionRuleFunction((p) -> true).build();
        assertTrue(evaluator.evaluate(policy).valid());

        evaluator = PolicyEvaluator.Builder.newInstance().permissionRuleFunction((p) -> false).build();
        assertFalse(evaluator.evaluate(policy).valid());

    }

    @Test
    void verifyProhibitionRuleFunctions() {
        var prohibition = Prohibition.Builder.newInstance().action(Action.Builder.newInstance().type("DENY").build()).build();
        var policy = Policy.Builder.newInstance().prohibition(prohibition).build();

        var evaluator = PolicyEvaluator.Builder.newInstance().prohibitionRuleFunction((p) -> true).build();
        assertFalse(evaluator.evaluate(policy).valid());  // prohibition triggered, fail

        evaluator = PolicyEvaluator.Builder.newInstance().prohibitionRuleFunction((p) -> false).build();
        assertTrue(evaluator.evaluate(policy).valid()); // prohibition not triggered, succeed

    }

    @Test
    void verifyDutyRuleFunctions() {
        var duty = Duty.Builder.newInstance().action(Action.Builder.newInstance().type("DELETE").build()).build();
        var policy = Policy.Builder.newInstance().duty(duty).build();

        var evaluator = PolicyEvaluator.Builder.newInstance().dutyRuleFunction((p) -> true).build();
        assertTrue(evaluator.evaluate(policy).valid());

        evaluator = PolicyEvaluator.Builder.newInstance().dutyRuleFunction((p) -> false).build();
        assertFalse(evaluator.evaluate(policy).valid());

    }
    
    @Test
    void evaluateResourceManifest_validResourceDefinition() {
        var definition = TestDefinition.Builder.newInstance().id("id").key("someValue").build();
        var manifest = ResourceManifest.Builder.newInstance().definitions(List.of(definition)).build();
        
        var evaluator = PolicyEvaluator.Builder.newInstance()
                .resourceDefinitionPermissionFunction(TestDefinition.class, (r, d) -> Result.success(definition))
                .build();
        
        var permission = Permission.Builder.newInstance().action(Action.Builder.newInstance().type("USE").build()).build();
        var policy = Policy.Builder.newInstance().permission(permission).build();
        
        var result = evaluator.evaluateManifest(manifest, policy);
        assertThat(result.succeeded()).isTrue();
    }
    
    @Test
    void evaluateResourceManifest_invalidResourceDefinition() {
        var errorMessage = "invalid";
        
        var definition = TestDefinition.Builder.newInstance().id("id").key("someValue").build();
        var manifest = ResourceManifest.Builder.newInstance().definitions(List.of(definition)).build();
    
        var evaluator = PolicyEvaluator.Builder.newInstance()
                .resourceDefinitionPermissionFunction(TestDefinition.class, (r, d) -> Result.failure(errorMessage))
                .build();
    
        var permission = Permission.Builder.newInstance().action(Action.Builder.newInstance().type("USE").build()).build();
        var policy = Policy.Builder.newInstance().permission(permission).build();
    
        var result = evaluator.evaluateManifest(manifest, policy);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1).containsExactly(errorMessage);
    }
    
    @ParameterizedTest
    @ArgumentsSource(RuleTypeArguments.class)
    void verifyResourceDefinitionRuleFunctions(Class<? extends Rule> ruleType) {
        var originalKey = "someValue";
        var newKey = "someOtherValue";
        
        var definition = TestDefinition.Builder.newInstance().id("id").key(originalKey).build();
        var manifest = ResourceManifest.Builder.newInstance().definitions(List.of(definition)).build();
    
        var policyBuilder = Policy.Builder.newInstance();
        var evaluatorBuilder = PolicyEvaluator.Builder.newInstance();
    
        // Create policy and configure evaluator for correct rule type
        if (ruleType.isAssignableFrom(Permission.class)) {
            var permission = Permission.Builder.newInstance()
                    .action(Action.Builder.newInstance().type("USE").build())
                    .build();
            policyBuilder.permission(permission);
        
            evaluatorBuilder.resourceDefinitionPermissionFunction(TestDefinition.class, (r, d) -> {
                d.setKey(newKey);
                return Result.success(d);
            });
        } else if (ruleType.isAssignableFrom(Prohibition.class)) {
            var prohibition = Prohibition.Builder.newInstance()
                    .action(Action.Builder.newInstance().type("USE").build())
                    .build();
            policyBuilder.prohibition(prohibition);
        
            evaluatorBuilder.resourceDefinitionProhibitionFunction(TestDefinition.class, (r, d) -> {
                d.setKey(newKey);
                return Result.success(d);
            });
        } else {
            var duty = Duty.Builder.newInstance()
                    .action(Action.Builder.newInstance().type("USE").build())
                    .build();
            policyBuilder.duty(duty);
        
            evaluatorBuilder.resourceDefinitionDutyFunction(TestDefinition.class, (r, d) -> {
                d.setKey(newKey);
                return Result.success(d);
            });
        }
        
        var result = evaluatorBuilder.build().evaluateManifest(manifest, policyBuilder.build());
        assertThat(result.succeeded()).isTrue();
        
        var evaluatedDefinitions = result.getContent().getDefinitions();
        assertThat(evaluatedDefinitions).hasSize(1);
        
        var evaluatedDefinition = (TestDefinition) evaluatedDefinitions.get(0);
        assertThat(evaluatedDefinition.getId()).isEqualTo(definition.getId());
        assertThat(evaluatedDefinition.getKey())
                .isNotEqualTo(originalKey)
                .isEqualTo(newKey);
    }

    @ParameterizedTest
    @ArgumentsSource(RuleTypeArguments.class)
    void verifyResourceDefinitionConstraintFunctions(Class<? extends Rule> ruleType) {
        var leftOperand = "leftOperand";
        var rightValue = "rightValue";
        var originalKey = "someValue";
    
        var definition = TestDefinition.Builder.newInstance().id("id").key(originalKey).build();
        var manifest = ResourceManifest.Builder.newInstance().definitions(List.of(definition)).build();
        
        var constraint = AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression(leftOperand))
                .operator(Operator.EQ)
                .rightExpression(new LiteralExpression(rightValue))
                .build();
        
        var policyBuilder = Policy.Builder.newInstance();
        var evaluatorBuilder = PolicyEvaluator.Builder.newInstance();
        
        // Create policy and configure evaluator for correct rule type
        if (ruleType.isAssignableFrom(Permission.class)) {
            var permission = Permission.Builder.newInstance()
                    .action(Action.Builder.newInstance().type("USE").build())
                    .constraint(constraint)
                    .build();
            policyBuilder.permission(permission);
            
            evaluatorBuilder.resourceDefinitionConstraintPermissionFunction(leftOperand, TestDefinition.class, (op, rv, r, d) -> {
                d.setKey(((LiteralExpression) rv).asString());
                return Result.success(d);
            });
        } else if (ruleType.isAssignableFrom(Prohibition.class)) {
            var prohibition = Prohibition.Builder.newInstance()
                    .action(Action.Builder.newInstance().type("USE").build())
                    .constraint(constraint)
                    .build();
            policyBuilder.prohibition(prohibition);
    
            evaluatorBuilder.resourceDefinitionConstraintProhibitionFunction(leftOperand, TestDefinition.class, (op, rv, r, d) -> {
                d.setKey(((LiteralExpression) rv).asString());
                return Result.success(d);
            });
        } else {
            var duty = Duty.Builder.newInstance()
                    .action(Action.Builder.newInstance().type("USE").build())
                    .constraint(constraint)
                    .build();
            policyBuilder.duty(duty);
    
            evaluatorBuilder.resourceDefinitionConstraintDutyFunction(leftOperand, TestDefinition.class, (op, rv, r, d) -> {
                d.setKey(((LiteralExpression) rv).asString());
                return Result.success(d);
            });
        }
    
        var result = evaluatorBuilder.build().evaluateManifest(manifest, policyBuilder.build());
        assertThat(result.succeeded()).isTrue();
    
        var evaluatedDefinitions = result.getContent().getDefinitions();
        assertThat(evaluatedDefinitions).hasSize(1);
    
        var evaluatedDefinition = (TestDefinition) evaluatedDefinitions.get(0);
        assertThat(evaluatedDefinition.getId()).isEqualTo(definition.getId());
        assertThat(evaluatedDefinition.getKey())
                .isNotEqualTo(originalKey)
                .isEqualTo(rightValue);
    }
    
    @Test
    void verifyResourceDefinitionConstraintFunction_otherLeftOperand() {
        var originalKey = "someValue";
        var otherKey = "otherValue";
    
        var definition = TestDefinition.Builder.newInstance().id("id").key(originalKey).build();
        var manifest = ResourceManifest.Builder.newInstance().definitions(List.of(definition)).build();
    
        var constraint = AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression("leftOperand"))
                .operator(Operator.EQ)
                .rightExpression(new LiteralExpression("rightValue"))
                .build();
    
        var policy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                    .action(Action.Builder.newInstance().type("USE").build())
                    .constraint(constraint)
                    .build())
                .build();
        var evaluator = PolicyEvaluator.Builder.newInstance()
                .resourceDefinitionConstraintPermissionFunction("registrationKey", TestDefinition.class, (op, rv, r, d) -> {
                    d.setKey(otherKey);
                    return Result.success(d);
                })
                .build();
    
        var result = evaluator.evaluateManifest(manifest, policy);
        assertThat(result.succeeded()).isTrue();
    
        var evaluatedDefinitions = result.getContent().getDefinitions();
        assertThat(evaluatedDefinitions).hasSize(1);
    
        var evaluatedDefinition = (TestDefinition) evaluatedDefinitions.get(0);
        assertThat(evaluatedDefinition.getId()).isEqualTo(definition.getId());
        assertThat(evaluatedDefinition.getKey())
                .isNotEqualTo(otherKey)
                .isEqualTo(originalKey);
    }
}
