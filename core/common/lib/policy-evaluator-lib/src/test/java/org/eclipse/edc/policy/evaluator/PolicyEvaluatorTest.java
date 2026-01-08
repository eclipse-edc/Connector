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
 *
 */

package org.eclipse.edc.policy.evaluator;

import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.AtomicConstraintFunction;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.eclipse.edc.policy.evaluator.PolicyTestFunctions.createLiteralAtomicConstraint;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PolicyEvaluatorTest {

    @Test
    void verifyPermissionEvaluationFails_whenNoFunctionWasDefined() {
        var constraint = createLiteralAtomicConstraint("foo", "foo");
        var permission = Permission.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().permission(permission).build();

        var evaluator = PolicyEvaluator.Builder.newInstance().build();
        assertFalse(evaluator.evaluate(policy).valid());
    }

    @Test
    void verifyProhibitionEvaluationFails_whenNoFunctionWasDefined() {
        var constraint = createLiteralAtomicConstraint("foo", "foo");
        var prohibition = Prohibition.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().prohibition(prohibition).build();

        var evaluator = PolicyEvaluator.Builder.newInstance().build();
        assertFalse(evaluator.evaluate(policy).valid());
    }

    @Test
    void verifyDutyEvaluationFails_whenNoFunctionWasDefined() {
        var constraint = createLiteralAtomicConstraint("foo", "foo");
        var duty = Duty.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().duty(duty).build();

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
    void verifyDynamicPermissionFunctions() {
        var constraint = createLiteralAtomicConstraint("toResolve", "foo");
        var permission = Permission.Builder.newInstance().constraint(constraint).build();

        var policy = Policy.Builder.newInstance().permission(permission).build();

        var evaluator = PolicyEvaluator.Builder.newInstance().dynamicPermissionFunction((key) -> true, (key, operator, value, p) -> true).build();
        assertTrue(evaluator.evaluate(policy).valid());

        evaluator = PolicyEvaluator.Builder.newInstance().dynamicPermissionFunction((key) -> true, (key, operator, value, p) -> false).build();
        assertFalse(evaluator.evaluate(policy).valid());

    }

    @Test
    void verifyDynamicPermissionFunctionsHierarchy() {
        var constraint = createLiteralAtomicConstraint("toResolve", "foo");
        var permission = Permission.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().permission(permission).build();

        var evaluator = PolicyEvaluator.Builder.newInstance()
                .permissionFunction("toResolve", (operator, value, p) -> false)
                .dynamicPermissionFunction((key) -> true, (key, operator, value, p) -> true).build();

        assertFalse(evaluator.evaluate(policy).valid());

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
    void verifyDynamicDutyFunctions() {
        var constraint = createLiteralAtomicConstraint("toResolve", "foo");
        var duty = Duty.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().duty(duty).build();

        var evaluator = PolicyEvaluator.Builder.newInstance().dynamicDutyFunction((key) -> true, (key, operator, value, p) -> true).build();
        assertTrue(evaluator.evaluate(policy).valid());

        evaluator = PolicyEvaluator.Builder.newInstance().dynamicDutyFunction((key) -> true, (key, operator, value, p) -> false).build();
        assertFalse(evaluator.evaluate(policy).valid());

    }

    @Test
    void verifyDynamicDutyFunctionsHierarchy() {
        var constraint = createLiteralAtomicConstraint("toResolve", "foo");
        var duty = Duty.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().duty(duty).build();

        var evaluator = PolicyEvaluator.Builder.newInstance()
                .dutyFunction("toResolve", (operator, value, p) -> false)
                .dynamicDutyFunction((key) -> true, (key, operator, value, p) -> true).build();

        assertFalse(evaluator.evaluate(policy).valid());

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
    void verifyDynamicProhibitionFunctions() {
        var constraint = createLiteralAtomicConstraint("toResolve", "foo");
        var prohibition = Prohibition.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().prohibition(prohibition).build();

        var evaluator = PolicyEvaluator.Builder.newInstance().dynamicProhibitionFunction((key) -> true, (key, operator, value, p) -> false).build();
        assertTrue(evaluator.evaluate(policy).valid());

        evaluator = PolicyEvaluator.Builder.newInstance().dynamicProhibitionFunction((key) -> true, (key, operator, value, p) -> true).build();
        assertFalse(evaluator.evaluate(policy).valid());

    }

    @Test
    void verifyDynamicProhibitionFunctionsHierarchy() {
        var constraint = createLiteralAtomicConstraint("toResolve", "foo");
        var prohibition = Prohibition.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().prohibition(prohibition).build();

        var evaluator = PolicyEvaluator.Builder.newInstance()
                .prohibitionFunction("toResolve", (operator, value, p) -> false)
                .dynamicProhibitionFunction((key) -> true, (key, operator, value, p) -> true).build();

        assertTrue(evaluator.evaluate(policy).valid());

    }

    @Test
    void verifyPermissionRuleFunctions() {
        var permission = Permission.Builder.newInstance().action(Action.Builder.newInstance().type("use").build()).build();
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
    void verifyAtomicConstrainComplexType() {
        var left = new LiteralExpression("test");
        var right = new LiteralExpression(List.of("one", "two", "three"));
        var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(Operator.EQ).rightExpression(right).build();

        var duty = Duty.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().duty(duty).build();

        @SuppressWarnings("unchecked") AtomicConstraintFunction<Object, Duty, Boolean> mock = mock(AtomicConstraintFunction.class);
        when(mock.evaluate(eq(Operator.EQ), isA(List.class), isA(Duty.class))).thenReturn(true);

        // verify that the constraint function is invoked and passed the collection
        var evaluator = PolicyEvaluator.Builder.newInstance().dutyFunction("test", mock).build();
        assertTrue(evaluator.evaluate(policy).valid());

        verify(mock).evaluate(eq(Operator.EQ), isA(List.class), isA(Duty.class));
    }

}
