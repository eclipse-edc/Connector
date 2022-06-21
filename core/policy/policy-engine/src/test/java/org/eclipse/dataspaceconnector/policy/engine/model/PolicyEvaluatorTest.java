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

package org.eclipse.dataspaceconnector.policy.engine.model;

import org.eclipse.dataspaceconnector.policy.engine.PolicyEvaluator;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.junit.jupiter.api.Test;

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
}
