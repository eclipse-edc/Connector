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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.policy.engine;

import org.eclipse.edc.policy.engine.spi.DynamicAtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.engine.validation.RuleValidator;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.policy.engine.spi.PolicyEngine.ALL_SCOPES;
import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PolicyEngineImplTest {

    private static final String TEST_SCOPE = "test";
    private final RuleBindingRegistry bindingRegistry = new RuleBindingRegistryImpl();
    private PolicyEngine policyEngine;


    @BeforeEach
    void setUp() {
        policyEngine = new PolicyEngineImpl(new ScopeFilter(bindingRegistry), new RuleValidator(bindingRegistry));
    }

    @Test
    void validateEmptyPolicy() {
        var context = PolicyContextImpl.Builder.newInstance().build();
        var emptyPolicy = Policy.Builder.newInstance().build();

        // No explicit rule specified, policy should evaluate to true
        var result = policyEngine.evaluate(TEST_SCOPE, emptyPolicy, context);

        assertThat(result).isSucceeded();
    }

    @Test
    void validateUnsatisfiedDuty() {
        var context = PolicyContextImpl.Builder.newInstance().build();
        bindingRegistry.bind("foo", ALL_SCOPES);

        policyEngine.registerFunction(ALL_SCOPES, Duty.class, "foo", (op, rv, duty, ctx) -> false);

        var left = new LiteralExpression("foo");
        var right = new LiteralExpression("bar");
        var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();
        var duty = Duty.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().duty(duty).build();

        // The duty is not satisfied, so the policy should evaluate to false
        var result = policyEngine.evaluate(TEST_SCOPE, policy, context);

        assertThat(result).isFailed();
    }

    @Test
    void validateRuleOutOfScope() {
        // Verifies that a rule will be filtered if its action is not registered. The constraint is registered but should be filtered since it is contained in the permission.
        // If the permission is not properly filtered, the constraint will not be fulfilled and raise an exception.
        bindingRegistry.bind("foo", ALL_SCOPES);
        var context = PolicyContextImpl.Builder.newInstance().build();

        var left = new LiteralExpression("foo");
        var right = new LiteralExpression("bar");
        var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();

        var action = Action.Builder.newInstance().type("OUT_OF_SCOPE").build();
        var permission = Permission.Builder.newInstance().action(action).constraint(constraint).build();
        var policy = Policy.Builder.newInstance().permission(permission).build();

        // the permission containing the unfulfilled constraint should be filtered, resulting in the policy evaluation succeeding
        var result = policyEngine.evaluate(TEST_SCOPE, policy, context);

        assertThat(result).isSucceeded();
    }

    @Test
    void validateUngrantedPermission() {
        bindingRegistry.bind("foo", ALL_SCOPES);

        policyEngine.registerFunction(ALL_SCOPES, Permission.class, "foo", (op, rv, duty, context) -> false);
        var context = PolicyContextImpl.Builder.newInstance().build();

        var left = new LiteralExpression("foo");
        var right = new LiteralExpression("bar");
        var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();
        var permission = Permission.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().permission(permission).build();

        // The permission is not granted, so the policy should evaluate to false
        var result = policyEngine.evaluate(TEST_SCOPE, policy, context);

        assertThat(result).isFailed();
    }

    @Test
    void validateTriggeredProhibition() {
        bindingRegistry.bind("foo", ALL_SCOPES);

        policyEngine.registerFunction(ALL_SCOPES, Prohibition.class, "foo", (op, rv, duty, context) -> true);
        var context = PolicyContextImpl.Builder.newInstance().build();

        var policy = createTestPolicy();

        // The prohibition is triggered (it is true), so the policy should evaluate to false
        var result = policyEngine.evaluate(TEST_SCOPE, policy, context);

        assertThat(result).isFailed();
    }

    @Test
    void validateConstraintFunctionOutOfScope() {
        bindingRegistry.bind("foo", ALL_SCOPES);

        policyEngine.registerFunction("foo", Prohibition.class, "foo", (op, rv, duty, context) -> Assertions.fail("Foo prohibition should be out of scope"));
        policyEngine.registerFunction("bar", Prohibition.class, "foo", (op, rv, duty, context) -> true);
        var context = PolicyContextImpl.Builder.newInstance().build();

        var policy = createTestPolicy();

        // The bar-scoped prohibition is triggered (it is true), so the policy should evaluate to false
        var result = policyEngine.evaluate("bar", policy, context);

        assertThat(result).isFailed();
    }

    @Test
    void validateChildScopeNotVisible() {
        bindingRegistry.bind("foo", ALL_SCOPES);

        policyEngine.registerFunction("bar", Prohibition.class, "foo", (op, rv, duty, context) -> true);
        policyEngine.registerFunction("bar.child", Prohibition.class, "foo", (op, rv, duty, context) -> Assertions.fail("Child prohibition should be out of scope"));
        var context = PolicyContextImpl.Builder.newInstance().build();

        var policy = createTestPolicy();

        // The bar-scoped prohibition is triggered (it is true), so the policy should evaluate to false
        var result = policyEngine.evaluate("bar", policy, context);

        assertThat(result).isFailed();
    }

    @Test
    void validateScopeIsInheritedByChildren() {
        bindingRegistry.bind("foo", ALL_SCOPES);

        policyEngine.registerFunction("bar", Prohibition.class, "foo", (op, rv, duty, context) -> true);
        var context = PolicyContextImpl.Builder.newInstance().build();

        var policy = createTestPolicy();

        // The bar-scoped prohibition is triggered (it is true), so the policy should evaluate to false
        var result = policyEngine.evaluate("bar.child", policy, context);

        assertThat(result).isFailed();
    }

    @Test
    void validateRuleFunctionOutOfScope() {
        bindingRegistry.bind("foo", ALL_SCOPES);

        var action = Action.Builder.newInstance().type("use").build();

        var permission = Permission.Builder.newInstance().action(action).build();

        var policy = Policy.Builder.newInstance().permission(permission).build();

        var context = PolicyContextImpl.Builder.newInstance().build();

        policyEngine.registerFunction("foo", Permission.class, (rule, ctx) -> Assertions.fail("Foo permission should be out of scope"));
        policyEngine.registerFunction("bar", Permission.class, (rule, ctx) -> rule.getAction().getType().equals(action.getType()));
        assertThat(policyEngine.evaluate("bar", policy, context).succeeded()).isTrue();
    }

    @Test
    void validateAllScopesPreFunctionalValidator() {
        bindingRegistry.bind("foo", ALL_SCOPES);

        BiFunction<Policy, PolicyContext, Boolean> function = (policy, context) -> false;
        policyEngine.registerPreValidator(ALL_SCOPES, function);

        var policy = Policy.Builder.newInstance().build();
        var context = PolicyContextImpl.Builder.newInstance().build();

        var result = policyEngine.evaluate(TEST_SCOPE, policy, context);

        assertThat(result).isFailed();
    }

    @Test
    void validateAllScopesPostFunctionalValidator() {
        bindingRegistry.bind("foo", ALL_SCOPES);

        BiFunction<Policy, PolicyContext, Boolean> function = (policy, context) -> false;
        policyEngine.registerPostValidator(ALL_SCOPES, function);

        var policy = Policy.Builder.newInstance().build();
        var context = PolicyContextImpl.Builder.newInstance().build();

        var result = policyEngine.evaluate(TEST_SCOPE, policy, context);

        assertThat(result).isFailed();
    }


    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void validateAllScopesPrePostValidator(boolean preValidation) {
        bindingRegistry.bind("foo", ALL_SCOPES);

        if (preValidation) {
            policyEngine.registerPreValidator(ALL_SCOPES, (policy, context) -> false);
        } else {
            policyEngine.registerPostValidator(ALL_SCOPES, (policy, context) -> false);
        }
        var policy = Policy.Builder.newInstance().build();
        var context = PolicyContextImpl.Builder.newInstance().build();

        var result = policyEngine.evaluate(TEST_SCOPE, policy, context);

        assertThat(result).isFailed();
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void validateScopedPrePostValidator(boolean preValidation) {
        bindingRegistry.bind("foo", TEST_SCOPE);

        if (preValidation) {
            policyEngine.registerPreValidator(TEST_SCOPE, (policy, context) -> false);
        } else {
            policyEngine.registerPostValidator(TEST_SCOPE, (policy, context) -> false);
        }

        var policy = Policy.Builder.newInstance().build();
        var context = PolicyContextImpl.Builder.newInstance().build();

        var result = policyEngine.evaluate(TEST_SCOPE, policy, context);

        assertThat(result).isFailed();
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void validateOutOfScopedPrePostValidator(boolean preValidation) {
        bindingRegistry.bind("foo", TEST_SCOPE);

        if (preValidation) {
            policyEngine.registerPreValidator("random.scope", (policy, context) -> false);
        } else {
            policyEngine.registerPostValidator("random.scope", (policy, context) -> false);
        }

        var policy = Policy.Builder.newInstance().build();
        var context = PolicyContextImpl.Builder.newInstance().build();

        var result = policyEngine.evaluate(TEST_SCOPE, policy, context);

        assertThat(result.succeeded()).isTrue();
    }


    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void validateHierarchicalScopedNotFiredPrePostValidator(boolean preValidation) {
        bindingRegistry.bind("foo", TEST_SCOPE);

        if (preValidation) {
            policyEngine.registerPreValidator(TEST_SCOPE + ".test", (policy, context) -> false);
        } else {
            policyEngine.registerPostValidator(TEST_SCOPE + ".test", (policy, context) -> false);
        }

        var policy = Policy.Builder.newInstance().build();
        var context = PolicyContextImpl.Builder.newInstance().build();

        var result = policyEngine.evaluate(TEST_SCOPE, policy, context);

        assertThat(result.succeeded()).isTrue();
    }


    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void validateHierarchicalScopedFiredPrePostValidator(boolean preValidation) {
        bindingRegistry.bind("foo", TEST_SCOPE);

        if (preValidation) {
            policyEngine.registerPreValidator(TEST_SCOPE, (policy, context) -> false);
        } else {
            policyEngine.registerPostValidator(TEST_SCOPE, (policy, context) -> false);
        }

        var policy = Policy.Builder.newInstance().build();
        var context = PolicyContextImpl.Builder.newInstance().build();

        var result = policyEngine.evaluate(TEST_SCOPE + ".test", policy, context);

        assertThat(result).isFailed();
    }

    @ParameterizedTest
    @ArgumentsSource(PolicyProvider.class)
    void shouldTriggerDynamicFunction_whenWildcardScope(Policy policy, Class<Rule> ruleClass, boolean evaluateReturn) {
        bindingRegistry.dynamicBind((key) -> Set.of(TEST_SCOPE));

        var context = PolicyContextImpl.Builder.newInstance().build();
        DynamicAtomicConstraintFunction<Rule> function = mock(DynamicAtomicConstraintFunction.class);
        policyEngine.registerFunction(ALL_SCOPES, ruleClass, function);

        when(function.canHandle(any())).thenReturn(true);
        when(function.evaluate(any(), any(), any(), any(), eq(context))).thenReturn(evaluateReturn);

        var result = policyEngine.evaluate(TEST_SCOPE, policy, context);

        assertThat(result.succeeded()).isTrue();

        verify(function).canHandle(any());
        verify(function).evaluate(any(), any(), any(), any(), eq(context));
    }

    @ParameterizedTest
    @ArgumentsSource(PolicyProvider.class)
    void shouldTriggerDynamicFunction_whenExplicitScope(Policy policy, Class<Rule> ruleClass, boolean evaluateReturn) {
        bindingRegistry.dynamicBind((key) -> Set.of(TEST_SCOPE));

        var context = PolicyContextImpl.Builder.newInstance().build();
        DynamicAtomicConstraintFunction<Rule> function = mock(DynamicAtomicConstraintFunction.class);
        policyEngine.registerFunction(TEST_SCOPE, ruleClass, function);

        when(function.canHandle(any())).thenReturn(true);
        when(function.evaluate(any(), any(), any(), any(), eq(context))).thenReturn(evaluateReturn);

        var result = policyEngine.evaluate(TEST_SCOPE, policy, context);

        assertThat(result.succeeded()).isTrue();

        verify(function).canHandle(any());
        verify(function).evaluate(any(), any(), any(), any(), eq(context));
    }

    @ParameterizedTest
    @ArgumentsSource(PolicyProvider.class)
    void shouldNotTriggerDynamicFunction_whenBindAlreadyAvailable(Policy policy, Class<Rule> ruleClass) {
        bindingRegistry.bind("foo", ALL_SCOPES);
        policyEngine.registerFunction(ALL_SCOPES, ruleClass, "foo", (op, rv, duty, context) -> !ruleClass.isAssignableFrom(Prohibition.class));
        bindingRegistry.dynamicBind((key) -> Set.of(TEST_SCOPE));

        var context = PolicyContextImpl.Builder.newInstance().build();
        DynamicAtomicConstraintFunction<Rule> function = mock(DynamicAtomicConstraintFunction.class);
        policyEngine.registerFunction(ALL_SCOPES, ruleClass, function);

        var result = policyEngine.evaluate(TEST_SCOPE, policy, context);

        assertThat(result.succeeded()).isTrue();

        verifyNoInteractions(function);
    }

    @ParameterizedTest
    @ArgumentsSource(PolicyProvider.class)
    void shouldNotTriggerDynamicFunction_whenDifferentScope(Policy policy, Class<Rule> ruleClass, boolean evaluateReturn) {
        bindingRegistry.dynamicBind((key) -> Set.of(TEST_SCOPE));

        var context = PolicyContextImpl.Builder.newInstance().build();
        DynamicAtomicConstraintFunction<Rule> function = mock(DynamicAtomicConstraintFunction.class);
        policyEngine.registerFunction(TEST_SCOPE, ruleClass, function);

        when(function.canHandle(any())).thenReturn(true);
        when(function.evaluate(any(), any(), any(), any(), eq(context))).thenReturn(evaluateReturn);

        var result = policyEngine.evaluate("randomScope", policy, context);

        assertThat(result.succeeded()).isTrue();

        verifyNoInteractions(function);
    }

    private Policy createTestPolicy() {
        var left = new LiteralExpression("foo");
        var right = new LiteralExpression("bar");
        var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();
        var prohibition = Prohibition.Builder.newInstance().constraint(constraint).build();
        return Policy.Builder.newInstance().prohibition(prohibition).build();
    }

    private static class PolicyProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

            var left = new LiteralExpression("foo");
            var right = new LiteralExpression("bar");
            var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();
            var prohibition = Prohibition.Builder.newInstance().constraint(constraint).build();
            var permission = Permission.Builder.newInstance().constraint(constraint).build();
            var duty = Duty.Builder.newInstance().constraint(constraint).build();

            return Stream.of(
                    of(Policy.Builder.newInstance().permission(permission).build(), Permission.class, true),
                    of(Policy.Builder.newInstance().duty(duty).build(), Duty.class, true),
                    of(Policy.Builder.newInstance().prohibition(prohibition).build(), Prohibition.class, false)
            );
        }
    }

}
