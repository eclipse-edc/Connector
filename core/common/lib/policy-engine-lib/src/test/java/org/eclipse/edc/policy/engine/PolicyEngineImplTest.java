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

import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.engine.spi.DynamicAtomicConstraintRuleFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.PolicyValidatorRule;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.engine.spi.RulePolicyFunction;
import org.eclipse.edc.policy.engine.validation.RuleValidator;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.policy.engine.spi.PolicyEngine.ALL_SCOPES;
import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PolicyEngineImplTest {

    private static final String TEST_SCOPE = "test";
    private static final String PARENT_SCOPE = "parent";
    private static final String CHILD_SCOPE = "parent.child";
    private static final String FOO_SCOPE = "foo";
    private final RuleBindingRegistry bindingRegistry = new RuleBindingRegistryImpl();
    private PolicyEngine policyEngine;

    @BeforeEach
    void setUp() {
        policyEngine = new PolicyEngineImpl(new ScopeFilter(bindingRegistry), new RuleValidator(bindingRegistry));
        policyEngine.registerScope(TEST_SCOPE, TestContext.class);
        policyEngine.registerScope(PARENT_SCOPE, ParentContext.class);
        policyEngine.registerScope(CHILD_SCOPE, ChildContext.class);
        policyEngine.registerScope(FOO_SCOPE, ChildContext.class);
    }

    @Test
    void validateEmptyPolicy() {
        var context = new TestContext();
        var emptyPolicy = Policy.Builder.newInstance().build();

        // No explicit rule specified, policy should evaluate to true
        var result = policyEngine.evaluate(emptyPolicy, context);

        assertThat(result).isSucceeded();
    }

    @Deprecated(since = "0.10.0")
    @Test
    void shouldThrowException_whenRegisterWithScopeNotRegistered() {
        assertThatThrownBy(() -> policyEngine.registerFunction(ALL_SCOPES, Rule.class, "key", mock()));
        assertThatThrownBy(() -> policyEngine.registerFunction("unregistered.scope", Rule.class, "key", mock()));
    }

    @Test
    void validateUnsatisfiedDuty() {
        var context = new TestContext();
        bindingRegistry.bind("foo", ALL_SCOPES);

        policyEngine.registerFunction(TestContext.class, Duty.class, "foo", (op, rv, duty, ctx) -> false);

        var left = new LiteralExpression("foo");
        var right = new LiteralExpression("bar");
        var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();
        var duty = Duty.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().duty(duty).build();

        // The duty is not satisfied, so the policy should evaluate to false
        var result = policyEngine.evaluate(policy, context);

        assertThat(result).isFailed();
    }

    @Test
    void validateRuleOutOfScope() {
        // Verifies that a rule will be filtered if its action is not registered. The constraint is registered but should be filtered since it is contained in the permission.
        // If the permission is not properly filtered, the constraint will not be fulfilled and raise an exception.
        bindingRegistry.bind("foo", ALL_SCOPES);
        var context = new TestContext();

        var left = new LiteralExpression("foo");
        var right = new LiteralExpression("bar");
        var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();

        var action = Action.Builder.newInstance().type("OUT_OF_SCOPE").build();
        var permission = Permission.Builder.newInstance().action(action).constraint(constraint).build();
        var policy = Policy.Builder.newInstance().permission(permission).build();

        // the permission containing the unfulfilled constraint should be filtered, resulting in the policy evaluation succeeding
        var result = policyEngine.evaluate(policy, context);

        assertThat(result).isSucceeded();
    }

    @Test
    void validateNotGrantedPermission() {
        bindingRegistry.bind("foo", ALL_SCOPES);

        policyEngine.registerFunction(TestContext.class, Permission.class, "foo", (op, rv, duty, context) -> false);
        var context = new TestContext();

        var left = new LiteralExpression("foo");
        var right = new LiteralExpression("bar");
        var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();
        var permission = Permission.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().permission(permission).build();

        // The permission is not granted, so the policy should evaluate to false
        var result = policyEngine.evaluate(policy, context);

        assertThat(result).isFailed();
    }

    @Test
    void validateTriggeredProhibition() {
        bindingRegistry.bind("foo", ALL_SCOPES);

        policyEngine.registerFunction(PolicyContext.class, Prohibition.class, "foo", (op, rv, duty, context) -> true);
        var context = new TestContext();

        var policy = createTestPolicy();

        // The prohibition is triggered (it is true), so the policy should evaluate to false
        var result = policyEngine.evaluate(policy, context);

        assertThat(result).isFailed();
    }

    @Test
    void validateConstraintFunctionOutOfScope() {
        bindingRegistry.bind("foo", ALL_SCOPES);

        policyEngine.registerFunction(FooContext.class, Prohibition.class, "foo", (op, rv, duty, context) -> fail("Foo prohibition should be out of scope"));
        policyEngine.registerFunction(TestContext.class, Prohibition.class, "foo", (op, rv, duty, context) -> true);
        var context = new TestContext();

        var policy = createTestPolicy();

        // The bar-scoped prohibition is triggered (it is true), so the policy should evaluate to false
        var result = policyEngine.evaluate(policy, context);

        assertThat(result).isFailed();
    }

    @Test
    void validateRuleFunctionOutOfScope() {
        bindingRegistry.bind("foo", ALL_SCOPES);

        var action = Action.Builder.newInstance().type("use").build();

        var permission = Permission.Builder.newInstance().action(action).build();

        var policy = Policy.Builder.newInstance().permission(permission).build();

        var context = new TestContext();

        policyEngine.registerFunction(FooContext.class, Permission.class, (rule, ctx) -> fail("Foo permission should be out of scope"));
        policyEngine.registerFunction(TestContext.class, Permission.class, (rule, ctx) -> rule.getAction().getType().equals(action.getType()));

        var result = policyEngine.evaluate(policy, context);

        assertThat(result).isSucceeded();
    }

    @Test
    void validateAllScopesPreFunctionalValidator() {
        bindingRegistry.bind("foo", ALL_SCOPES);

        PolicyValidatorRule<PolicyContext> function = (policy, context) -> false;
        policyEngine.registerPreValidator(PolicyContext.class, function);

        var policy = Policy.Builder.newInstance().build();
        var context = new TestContext();

        var result = policyEngine.evaluate(policy, context);

        assertThat(result).isFailed();
    }

    @Test
    void validateAllScopesPostFunctionalValidator() {
        bindingRegistry.bind("foo", ALL_SCOPES);

        PolicyValidatorRule<PolicyContext> function = (policy, context) -> false;
        policyEngine.registerPostValidator(PolicyContext.class, function);

        var policy = Policy.Builder.newInstance().build();
        var context = new TestContext();

        var result = policyEngine.evaluate(policy, context);

        assertThat(result).isFailed();
    }


    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void validateAllScopesPrePostValidator(boolean preValidation) {
        bindingRegistry.bind("foo", ALL_SCOPES);

        if (preValidation) {
            policyEngine.registerPreValidator(PolicyContext.class, (policy, context) -> false);
        } else {
            policyEngine.registerPostValidator(PolicyContext.class, (policy, context) -> false);
        }
        var policy = Policy.Builder.newInstance().build();
        var context = new TestContext();

        var result = policyEngine.evaluate(policy, context);

        assertThat(result).isFailed();
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void validateScopedPrePostValidator(boolean preValidation) {
        bindingRegistry.bind("foo", TEST_SCOPE);

        if (preValidation) {
            policyEngine.registerPreValidator(TestContext.class, (policy, context) -> false);
        } else {
            policyEngine.registerPostValidator(TestContext.class, (policy, context) -> false);
        }

        var policy = Policy.Builder.newInstance().build();
        var context = new TestContext();

        var result = policyEngine.evaluate(policy, context);

        assertThat(result).isFailed();
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void validateOutOfScopedPrePostValidator(boolean preValidation) {
        bindingRegistry.bind("foo", TEST_SCOPE);

        if (preValidation) {
            policyEngine.registerPreValidator(FooContext.class, (policy, context) -> false);
        } else {
            policyEngine.registerPostValidator(FooContext.class, (policy, context) -> false);
        }

        var policy = Policy.Builder.newInstance().build();
        var context = new TestContext();

        var result = policyEngine.evaluate(policy, context);

        assertThat(result.succeeded()).isTrue();
    }


    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void validateHierarchicalScopedNotFiredPrePostValidator(boolean preValidation) {
        bindingRegistry.bind("foo", TEST_SCOPE);

        if (preValidation) {
            policyEngine.registerPreValidator(ChildContext.class, (policy, context) -> false);
        } else {
            policyEngine.registerPostValidator(ChildContext.class, (policy, context) -> false);
        }

        var policy = Policy.Builder.newInstance().build();
        var context = new ParentContext();

        var result = policyEngine.evaluate(policy, context);

        assertThat(result.succeeded()).isTrue();
    }


    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void validateHierarchicalScopedFiredPrePostValidator(boolean preValidation) {
        bindingRegistry.bind("foo", PARENT_SCOPE);

        if (preValidation) {
            policyEngine.registerPreValidator(ParentContext.class, (policy, context) -> false);
        } else {
            policyEngine.registerPostValidator(ParentContext.class, (policy, context) -> false);
        }

        var policy = Policy.Builder.newInstance().build();
        var context = new ChildContext();

        var result = policyEngine.evaluate(policy, context);

        assertThat(result).isFailed();
    }

    @ParameterizedTest
    @ArgumentsSource(PolicyProvider.class)
    void shouldTriggerDynamicFunction_whenWildcardScope(Policy policy, Class<Rule> ruleClass, boolean evaluateReturn) {
        bindingRegistry.dynamicBind((key) -> Set.of(TEST_SCOPE));

        var context = new TestContext();
        DynamicAtomicConstraintRuleFunction<Rule, PolicyContext> function = mock();
        policyEngine.registerFunction(PolicyContext.class, ruleClass, function);

        when(function.canHandle(any())).thenReturn(true);
        when(function.evaluate(any(), any(), any(), any(), eq(context))).thenReturn(evaluateReturn);

        var result = policyEngine.evaluate(policy, context);

        assertThat(result.succeeded()).isTrue();

        verify(function).canHandle(any());
        verify(function).evaluate(any(), any(), any(), any(), eq(context));
    }

    @ParameterizedTest
    @ArgumentsSource(PolicyProvider.class)
    void shouldTriggerDynamicFunction_whenExplicitScope(Policy policy, Class<Rule> ruleClass, boolean evaluateReturn) {
        bindingRegistry.dynamicBind((key) -> Set.of(TEST_SCOPE));

        var context = new TestContext();
        DynamicAtomicConstraintRuleFunction<Rule, TestContext> function = mock();
        policyEngine.registerFunction(TestContext.class, ruleClass, function);

        when(function.canHandle(any())).thenReturn(true);
        when(function.evaluate(any(), any(), any(), any(), eq(context))).thenReturn(evaluateReturn);

        var result = policyEngine.evaluate(policy, context);

        assertThat(result.succeeded()).isTrue();

        verify(function).canHandle(any());
        verify(function).evaluate(any(), any(), any(), any(), eq(context));
    }

    @ParameterizedTest
    @ArgumentsSource(PolicyProvider.class)
    void shouldNotTriggerDynamicFunction_whenBindAlreadyAvailable(Policy policy, Class<Rule> ruleClass) {
        bindingRegistry.bind("foo", ALL_SCOPES);
        policyEngine.registerFunction(PolicyContext.class, ruleClass, "foo", (op, rv, duty, context) -> !ruleClass.isAssignableFrom(Prohibition.class));
        bindingRegistry.dynamicBind((key) -> Set.of(TEST_SCOPE));

        var context = new TestContext();
        DynamicAtomicConstraintRuleFunction<Rule, PolicyContext> function = mock();
        policyEngine.registerFunction(PolicyContext.class, ruleClass, function);

        var result = policyEngine.evaluate(policy, context);

        assertThat(result.succeeded()).isTrue();

        verifyNoInteractions(function);
    }

    @ParameterizedTest
    @ArgumentsSource(PolicyProvider.class)
    void shouldNotTriggerDynamicFunction_whenDifferentScope(Policy policy, Class<Rule> ruleClass, boolean evaluateReturn) {
        bindingRegistry.dynamicBind((key) -> Set.of(TEST_SCOPE));

        var context = new FooContext();
        DynamicAtomicConstraintRuleFunction<Rule, TestContext> function = mock();
        policyEngine.registerFunction(TestContext.class, ruleClass, function);

        when(function.canHandle(any())).thenReturn(true);
        when(function.evaluate(any(), any(), any(), any(), any())).thenReturn(evaluateReturn);

        var result = policyEngine.evaluate(policy, context);

        assertThat(result.succeeded()).isTrue();

        verifyNoInteractions(function);
    }

    @Nested
    class TypedContext {

        @Test
        void shouldUseTypedContextOnAtomicConstraintFunction() {
            bindingRegistry.bind("foo", ALL_SCOPES);

            var left = new LiteralExpression("foo");
            var right = new LiteralExpression("bar");
            var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();
            var duty = Duty.Builder.newInstance().constraint(constraint).build();
            var policy = Policy.Builder.newInstance().duty(duty).build();

            AtomicConstraintRuleFunction<Duty, TestContext> function = mock();
            when(function.evaluate(any(), any(), any(), any())).thenReturn(false);
            policyEngine.registerFunction(TestContext.class, Duty.class, "foo", function);

            var context = new TestContext();

            var result = policyEngine.evaluate(policy, context);

            assertThat(result).isFailed();
            verify(function).evaluate(any(), any(), any(), same(context));
        }

        @Test
        void shouldUseTypedContextOnDynamicConstraintFunction() {
            bindingRegistry.bind("foo", ALL_SCOPES);

            var left = new LiteralExpression("foo");
            var right = new LiteralExpression("bar");
            var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();
            var duty = Duty.Builder.newInstance().constraint(constraint).build();
            var policy = Policy.Builder.newInstance().duty(duty).build();

            DynamicAtomicConstraintRuleFunction<Duty, TestContext> function = mock();
            when(function.canHandle(any())).thenReturn(true);

            policyEngine.registerFunction(TestContext.class, Duty.class, function);

            var context = new TestContext();

            var result = policyEngine.evaluate(policy, context);

            assertThat(result).isFailed();
            verify(function).evaluate(any(), any(), any(), any(), same(context));
        }

        @Test
        void shouldUseTypedContextOnRuleFunction() {
            bindingRegistry.bind("foo", ALL_SCOPES);

            var left = new LiteralExpression("foo");
            var right = new LiteralExpression("bar");
            var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();
            var duty = Duty.Builder.newInstance().constraint(constraint).build();
            var policy = Policy.Builder.newInstance().duty(duty).build();

            RulePolicyFunction<Duty, TestContext> function = mock();

            policyEngine.registerFunction(TestContext.class, Duty.class, function);

            var context = new TestContext();

            var result = policyEngine.evaluate(policy, context);

            assertThat(result).isFailed();
            verify(function).evaluate(any(), same(context));
        }

        @Test
        void validateChildScopeNotVisible() {
            bindingRegistry.bind("foo", ALL_SCOPES);

            AtomicConstraintRuleFunction<Prohibition, ParentContext> parentFunction = mock();
            when(parentFunction.evaluate(any(), any(), any(), any())).thenReturn(true);
            AtomicConstraintRuleFunction<Prohibition, ChildContext> childFunction = mock();
            when(childFunction.evaluate(any(), any(), any(), any())).thenReturn(true);
            policyEngine.registerFunction(ParentContext.class, Prohibition.class, "foo", parentFunction);
            policyEngine.registerFunction(ChildContext.class, Prohibition.class, "foo", childFunction);
            var context = new ParentContext();

            var policy = createTestPolicy();

            var result = policyEngine.evaluate(policy, context);

            assertThat(result).isFailed();
            verify(parentFunction).evaluate(any(), any(), any(), same(context));
            verifyNoInteractions(childFunction);
        }

        @Test
        void validateScopeIsInheritedByChildren() {
            bindingRegistry.bind("foo", ALL_SCOPES);
            AtomicConstraintRuleFunction<Prohibition, ParentContext> parentFunction = mock();
            when(parentFunction.evaluate(any(), any(), any(), any())).thenReturn(true);
            policyEngine.registerFunction(ParentContext.class, Prohibition.class, "foo", parentFunction);

            var context = new ChildContext();
            var policy = createTestPolicy();

            var result = policyEngine.evaluate(policy, context);

            assertThat(result).isFailed();
            verify(parentFunction).evaluate(any(), any(), any(), same(context));
        }

    }

    private static class TestContext extends PolicyContextImpl {
        @Override
        public String scope() {
            return TEST_SCOPE;
        }
    }

    private static class FooContext extends PolicyContextImpl {
        @Override
        public String scope() {
            return FOO_SCOPE;
        }
    }

    private static class ChildContext extends ParentContext {
        @Override
        public String scope() {
            return CHILD_SCOPE;
        }
    }

    private static class ParentContext extends PolicyContextImpl {
        @Override
        public String scope() {
            return PARENT_SCOPE;
        }
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
