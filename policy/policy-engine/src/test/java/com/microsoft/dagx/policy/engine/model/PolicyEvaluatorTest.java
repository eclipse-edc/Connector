/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.policy.engine.model;

import com.microsoft.dagx.policy.engine.PolicyEvaluator;
import com.microsoft.dagx.policy.model.AtomicConstraint;
import com.microsoft.dagx.policy.model.Duty;
import com.microsoft.dagx.policy.model.Permission;
import com.microsoft.dagx.policy.model.Policy;
import com.microsoft.dagx.policy.model.Prohibition;
import org.junit.jupiter.api.Test;

import static com.microsoft.dagx.policy.engine.model.PolicyTestFunctions.createLiteralAtomicConstraint;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
class PolicyEvaluatorTest {

    @Test
    void verifySimpleEval() {
        AtomicConstraint constraint = createLiteralAtomicConstraint("foo", "foo");

        Duty duty = Duty.Builder.newInstance().constraint(constraint).build();
        Policy policy = Policy.Builder.newInstance().duty(duty).build();

        PolicyEvaluator evaluator = PolicyEvaluator.Builder.newInstance().build();
        assertTrue(evaluator.evaluate(policy).valid());
    }

    @Test
    void verifyProhibitionNotEqualEval() {
        AtomicConstraint constraint = createLiteralAtomicConstraint("baz", "bar");

        Prohibition prohibition = Prohibition.Builder.newInstance().constraint(constraint).build();
        Policy policy = Policy.Builder.newInstance().prohibition(prohibition).build();

        PolicyEvaluator evaluator = PolicyEvaluator.Builder.newInstance().build();
        assertTrue(evaluator.evaluate(policy).valid());
    }

    @Test
    void verifyPermissionNotEqualEval() {
        AtomicConstraint constraint = createLiteralAtomicConstraint("baz", "bar");

        Permission permission = Permission.Builder.newInstance().constraint(constraint).build();
        Policy policy = Policy.Builder.newInstance().permission(permission).build();

        PolicyEvaluator evaluator = PolicyEvaluator.Builder.newInstance().build();
        assertFalse(evaluator.evaluate(policy).valid());
    }

    @Test
    void verifyPermissionFunctions() {
        AtomicConstraint constraint = createLiteralAtomicConstraint("toResolve", "foo");

        Permission permission = Permission.Builder.newInstance().constraint(constraint).build();
        Policy policy = Policy.Builder.newInstance().permission(permission).build();

        PolicyEvaluator evaluator = PolicyEvaluator.Builder.newInstance().permissionFunction("toResolve", (operator, value, p) -> "foo".equals(value)).build();
        assertTrue(evaluator.evaluate(policy).valid());
    }

    @Test
    void verifyDutyFunctions() {
        AtomicConstraint constraint = createLiteralAtomicConstraint("toResolve", "foo");

        Duty duty = Duty.Builder.newInstance().constraint(constraint).build();
        Policy policy = Policy.Builder.newInstance().duty(duty).build();

        PolicyEvaluator evaluator = PolicyEvaluator.Builder.newInstance().dutyFunction("toResolve", (operator, value, d) -> "foo".equals(value)).build();
        assertTrue(evaluator.evaluate(policy).valid());
    }

    @Test
    void verifyProhibitionFunctions() {
        AtomicConstraint constraint = createLiteralAtomicConstraint("toResolve", "foo");

        Prohibition prohibition = Prohibition.Builder.newInstance().constraint(constraint).build();
        Policy policy = Policy.Builder.newInstance().prohibition(prohibition).build();

        PolicyEvaluator evaluator = PolicyEvaluator.Builder.newInstance().prohibitionFunction("toResolve", (operator, value, pr) -> !"foo".equals(value)).build();
        assertTrue(evaluator.evaluate(policy).valid());
    }
}
