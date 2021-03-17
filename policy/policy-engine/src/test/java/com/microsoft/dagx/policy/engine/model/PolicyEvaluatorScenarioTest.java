package com.microsoft.dagx.policy.engine.model;

import com.microsoft.dagx.policy.engine.PolicyEvaluator;
import org.junit.jupiter.api.Test;

import static com.microsoft.dagx.policy.engine.model.PolicyTestFunctions.createLiteralAtomicConstraint;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
class PolicyEvaluatorScenarioTest {

    /**
     * Atomic constraint function models the case where an asset can only be used in the EU.
     */
    @Test
    void geospatialPermissionConstraint() {
        AtomicConstraint constraint = createLiteralAtomicConstraint("spatial", "EU");

        Permission permission = Permission.Builder.newInstance().constraint(constraint).build();
        Policy policy = Policy.Builder.newInstance().permission(permission).build();

        PolicyEvaluator evaluator = PolicyEvaluator.Builder.newInstance().permissionFunction("spatial", (operator, value, p) -> "EU".equals(value)).build();
        assertTrue(evaluator.evaluate(policy).valid());
    }

    /**
     * Atomic constraint function models the case where an asset is used by a client only authorized in the EU, and there is a permission that requires the
     * client to be authorized in both the EU and US.
     */
    @Test
    void geospatialPermissionInTwoRegionsConstraint() {
        AtomicConstraint constraintEu = createLiteralAtomicConstraint("spatial", "EU");
        AtomicConstraint constraintUs = createLiteralAtomicConstraint("spatial", "US");

        AndConstraint andConstraint = AndConstraint.Builder.newInstance().constraint(constraintEu).constraint(constraintUs).build();
        Permission permission = Permission.Builder.newInstance().constraint(andConstraint).build();

        Policy policy = Policy.Builder.newInstance().permission(permission).build();

        PolicyEvaluator evaluator = PolicyEvaluator.Builder.newInstance().permissionFunction("spatial", (operator, value, p) -> "EU".equals(value)).build();
        assertFalse(evaluator.evaluate(policy).valid());
    }

    /**
     * Atomic constraint function models the case where an asset is intended to be used in the EU, but there is a prohibition to do so.
     */
    @Test
    void geospatialProhibitionConstraint() {
        AtomicConstraint constraint = createLiteralAtomicConstraint("spatial", "EU");

        Prohibition prohibition = Prohibition.Builder.newInstance().constraint(constraint).build();
        Policy policy = Policy.Builder.newInstance().prohibition(prohibition).build();

        PolicyEvaluator evaluator = PolicyEvaluator.Builder.newInstance().prohibitionFunction("spatial", (operator, value, pr) -> "EU".equals(value)).build();
        assertFalse(evaluator.evaluate(policy).valid());
    }


}
