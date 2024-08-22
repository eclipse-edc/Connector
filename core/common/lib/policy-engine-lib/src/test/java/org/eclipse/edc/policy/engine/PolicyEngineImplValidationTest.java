/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.policy.engine;

import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.DynamicAtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.engine.validation.RuleValidator;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.policy.model.XoneConstraint;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.policy.engine.spi.PolicyEngine.ALL_SCOPES;
import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyEngineImplValidationTest {

    private final RuleBindingRegistry bindingRegistry = new RuleBindingRegistryImpl();
    private PolicyEngine policyEngine;


    @BeforeEach
    void setUp() {
        policyEngine = new PolicyEngineImpl(new ScopeFilter(bindingRegistry), new RuleValidator(bindingRegistry));
    }

    @Test
    void validateEmptyPolicy() {
        var emptyPolicy = Policy.Builder.newInstance().build();

        var result = policyEngine.validate(emptyPolicy);

        assertThat(result).isSucceeded();
    }

    @Test
    void validate_whenKeyNotBoundInTheRegistryAndToFunctions() {

        var left = new LiteralExpression("foo");
        var right = new LiteralExpression("bar");
        var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();
        var permission = Permission.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().permission(permission).build();
        policyEngine.registerFunction(ALL_SCOPES, Duty.class, "foo", (op, rv, r, ctx) -> true);
        policyEngine.registerFunction(ALL_SCOPES, Prohibition.class, "foo", (op, rv, r, ctx) -> true);

        var result = policyEngine.validate(policy);

        // The foo key is not bound nor to function nor in the RuleBindingRegistry
        assertThat(result).isFailed().messages().hasSize(2)
                .anyMatch(s -> s.startsWith("leftOperand 'foo' is not bound to any scopes"))
                .anyMatch(s -> s.startsWith("left operand 'foo' is not bound to any functions"));

    }

    @ParameterizedTest
    @ArgumentsSource(PolicyProvider.class)
    void validate_whenKeyIsNotBoundInTheRegistry(Policy policy, Class<Rule> ruleClass, String key) {

        policyEngine.registerFunction(ALL_SCOPES, ruleClass, key, (op, rv, duty, ctx) -> true);

        var result = policyEngine.validate(policy);

        // The input key is not bound in the RuleBindingRegistry
        assertThat(result).isFailed().messages().hasSize(1)
                .anyMatch(s -> s.startsWith("leftOperand '%s' is not bound to any scopes".formatted(key)));

    }

    @ParameterizedTest
    @ArgumentsSource(PolicyProvider.class)
    void validate(Policy policy, Class<Rule> ruleClass, String key) {


        bindingRegistry.bind(key, ALL_SCOPES);
        policyEngine.registerFunction(ALL_SCOPES, ruleClass, key, (op, rv, duty, ctx) -> true);

        var result = policyEngine.validate(policy);

        // The input key is not bound in the RuleBindingRegistry
        assertThat(result).isSucceeded();

    }


    @ParameterizedTest
    @ArgumentsSource(PolicyProvider.class)
    void validate_withDynamicFunction(Policy policy, Class<Rule> ruleClass, String key) {

        DynamicAtomicConstraintFunction<Rule> function = mock();

        when(function.canHandle(key)).thenReturn(true);

        when(function.validate(any(), any(), any(), any())).thenReturn(Result.success());

        bindingRegistry.dynamicBind(s -> Set.of(ALL_SCOPES));
        policyEngine.registerFunction(ALL_SCOPES, ruleClass, function);

        var result = policyEngine.validate(policy);

        // The input key is not bound in the RuleBindingRegistry
        assertThat(result).isSucceeded();

    }

    @ParameterizedTest
    @ArgumentsSource(PolicyProvider.class)
    void validate_shouldFail_whenSkippingDynamicFunction(Policy policy, Class<Rule> ruleClass, String key) {

        DynamicAtomicConstraintFunction<Rule> function = mock();

        when(function.canHandle(key)).thenReturn(false);

        bindingRegistry.dynamicBind(s -> Set.of(ALL_SCOPES));
        policyEngine.registerFunction(ALL_SCOPES, ruleClass, function);

        var result = policyEngine.validate(policy);

        // The input key is not bound any functions , the dynamic one cannot handle the input key
        assertThat(result).isFailed().messages().hasSize(1)
                .anyMatch(s -> s.startsWith("left operand '%s' is not bound to any functions".formatted(key)));

    }

    @ParameterizedTest
    @ArgumentsSource(PolicyProvider.class)
    void validate_shouldFails_withDynamicFunction(Policy policy, Class<Rule> ruleClass, String key) {

        DynamicAtomicConstraintFunction<Rule> function = mock();

        when(function.canHandle(key)).thenReturn(true);

        when(function.validate(any(), any(), any(), any())).thenReturn(Result.failure("Dynamic function validation failure"));

        bindingRegistry.dynamicBind(s -> Set.of(ALL_SCOPES));
        policyEngine.registerFunction(ALL_SCOPES, ruleClass, function);

        var result = policyEngine.validate(policy);

        assertThat(result).isFailed().detail().contains("Dynamic function validation failure");

    }


    @ParameterizedTest
    @ArgumentsSource(PolicyProvider.class)
    void validate_shouldFail_whenFunctionValidationFails(Policy policy, Class<Rule> ruleClass, String key) {

        AtomicConstraintFunction<Rule> function = mock();

        when(function.validate(any(), any(), any())).thenReturn(Result.failure("Function validation failure"));

        bindingRegistry.bind(key, ALL_SCOPES);
        policyEngine.registerFunction(ALL_SCOPES, ruleClass, key, function);

        var result = policyEngine.validate(policy);

        // The foo key is not bound in the RuleBindingRegistry
        assertThat(result).isFailed().detail().contains("Function validation failure");

    }

    @Test
    void validate_shouldFail_whenActionIsNotBound() {

        var leftOperand = "foo";
        var left = new LiteralExpression(leftOperand);
        var right = new LiteralExpression("bar");
        var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();
        var permission = Permission.Builder.newInstance().constraint(constraint).action(Action.Builder.newInstance().type("use").build()).build();

        var policy = Policy.Builder.newInstance().permission(permission).build();
        AtomicConstraintFunction<Permission> function = mock();

        when(function.validate(any(), any(), any())).thenReturn(Result.success());

        bindingRegistry.bind("foo", ALL_SCOPES);
        policyEngine.registerFunction(ALL_SCOPES, Permission.class, "foo", function);

        var result = policyEngine.validate(policy);

        // The use action is not bound in the RuleBindingRegistry
        assertThat(result).isFailed().detail().contains("action 'use' is not bound to any scopes");

    }

    @ParameterizedTest
    @ArgumentsSource(PolicyWithMultiplicityConstraintProvider.class)
    void validate_withMultiplicityConstraints(Policy policy, Class<Rule> ruleClass, String[] keys) {


        for (var key : keys) {
            bindingRegistry.bind(key, ALL_SCOPES);
            policyEngine.registerFunction(ALL_SCOPES, ruleClass, key, (op, rv, duty, ctx) -> true);
        }


        var result = policyEngine.validate(policy);

        // The foo key is not bound in the RuleBindingRegistry
        assertThat(result).isSucceeded();

    }

    private static class PolicyProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

            var leftOperand = "foo";
            var left = new LiteralExpression(leftOperand);
            var right = new LiteralExpression("bar");
            var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();
            var prohibition = Prohibition.Builder.newInstance().constraint(constraint).build();
            var permission = Permission.Builder.newInstance().constraint(constraint).build();
            var duty = Duty.Builder.newInstance().constraint(constraint).build();

            return Stream.of(
                    of(Policy.Builder.newInstance().permission(permission).build(), Permission.class, leftOperand),
                    of(Policy.Builder.newInstance().duty(duty).build(), Duty.class, leftOperand),
                    of(Policy.Builder.newInstance().prohibition(prohibition).build(), Prohibition.class, leftOperand)
            );
        }
    }

    private static class PolicyWithMultiplicityConstraintProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            var keys = new String[]{ "foo", "baz" };
            var firstConstraint = atomicConstraint("foo", "bar");
            var secondConstraint = atomicConstraint("baz", "bar");


            var orConstraints = OrConstraint.Builder.newInstance().constraint(firstConstraint).constraint(secondConstraint).build();
            var andConstraints = AndConstraint.Builder.newInstance().constraint(firstConstraint).constraint(secondConstraint).build();
            var xoneConstraint = XoneConstraint.Builder.newInstance().constraint(firstConstraint).constraint(secondConstraint).build();

            var prohibition = Prohibition.Builder.newInstance().constraint(orConstraints).build();
            var permission = Permission.Builder.newInstance().constraint(andConstraints).build();
            var duty = Duty.Builder.newInstance().constraint(xoneConstraint).build();

            return Stream.of(
                    of(Policy.Builder.newInstance().permission(permission).build(), Permission.class, keys),
                    of(Policy.Builder.newInstance().duty(duty).build(), Duty.class, keys),
                    of(Policy.Builder.newInstance().prohibition(prohibition).build(), Prohibition.class, keys)
            );
        }

        private AtomicConstraint atomicConstraint(String key, String value) {
            var left = new LiteralExpression(key);
            var right = new LiteralExpression(value);
            return AtomicConstraint.Builder.newInstance()
                    .leftExpression(left)
                    .operator(EQ)
                    .rightExpression(right)
                    .build();
        }
    }
}
