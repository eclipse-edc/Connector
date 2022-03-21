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
package org.eclipse.dataspaceconnector.core.base.policy;

import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.policy.model.Operator.EQ;
import static org.eclipse.dataspaceconnector.spi.policy.PolicyEngine.ALL_SCOPES;

class PolicyEngineImplTest {
    private static final String TEST_SCOPE = "test";
    private PolicyEngineImpl policyEngine;

    @Test
    void validateEmptyPolicy() {
        var agent = new ParticipantAgent(emptyMap(), emptyMap());

        var emptyPolicy = Policy.Builder.newInstance().build();

        // No explicit rule specified, policy should evaluate to true
        var result = policyEngine.evaluate(TEST_SCOPE, emptyPolicy, agent);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validateUnsatisfiedDuty() {
        policyEngine.registerFunction(ALL_SCOPES, Duty.class, "foo", (op, rv, duty, context) -> false);
        var agent = new ParticipantAgent(emptyMap(), emptyMap());

        var left = new LiteralExpression("foo");
        var right = new LiteralExpression("bar");
        var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();
        var duty = Duty.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().duty(duty).build();

        // The duty is not satisfied, so the policy should evaluate to false
        assertThat(policyEngine.evaluate(TEST_SCOPE, policy, agent).succeeded()).isFalse();
    }

    @Test
    void validateUngrantedPermission() {
        policyEngine.registerFunction(ALL_SCOPES, Permission.class, "foo", (op, rv, duty, context) -> false);
        var agent = new ParticipantAgent(emptyMap(), emptyMap());

        var left = new LiteralExpression("foo");
        var right = new LiteralExpression("bar");
        var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();
        var permission = Permission.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().permission(permission).build();

        // The permission is not granted, so the policy should evaluate to false
        var result = policyEngine.evaluate(TEST_SCOPE, policy, agent);

        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void validateTriggeredProhibition() {
        policyEngine.registerFunction(ALL_SCOPES, Prohibition.class, "foo", (op, rv, duty, context) -> true);
        var agent = new ParticipantAgent(emptyMap(), emptyMap());

        Policy policy = createTestPolicy();

        // The prohibition is triggered (it is true), so the policy should evaluate to false
        var result = policyEngine.evaluate(TEST_SCOPE, policy, agent);

        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void validateConstraintFunctionOutOfScope() {
        policyEngine.registerFunction("foo", Prohibition.class, "foo", (op, rv, duty, context) -> Assertions.fail("Foo prohibition should be out of scope"));
        policyEngine.registerFunction("bar", Prohibition.class, "foo", (op, rv, duty, context) -> true);
        var agent = new ParticipantAgent(emptyMap(), emptyMap());

        Policy policy = createTestPolicy();

        // The bar-scoped prohibition is triggered (it is true), so the policy should evaluate to false
        var result = policyEngine.evaluate("bar", policy, agent);

        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void validateChildScopeNotVisible() {
        policyEngine.registerFunction("bar", Prohibition.class, "foo", (op, rv, duty, context) -> true);
        policyEngine.registerFunction("bar.child", Prohibition.class, "foo", (op, rv, duty, context) -> Assertions.fail("Child prohibition should be out of scope"));
        var agent = new ParticipantAgent(emptyMap(), emptyMap());

        Policy policy = createTestPolicy();

        // The bar-scoped prohibition is triggered (it is true), so the policy should evaluate to false
        var result = policyEngine.evaluate("bar", policy, agent);

        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void validateScopeIsInheritedByChildren() {
        policyEngine.registerFunction("bar", Prohibition.class, "foo", (op, rv, duty, context) -> true);
        var agent = new ParticipantAgent(emptyMap(), emptyMap());

        Policy policy = createTestPolicy();

        // The bar-scoped prohibition is triggered (it is true), so the policy should evaluate to false
        var result = policyEngine.evaluate("bar.child", policy, agent);

        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void validateRuleFunctionOutOfScope() {
        var  action = Action.Builder.newInstance().type("USE").build();

        var permission = Permission.Builder.newInstance().action(action).build();

        var policy = Policy.Builder.newInstance().permission(permission).build();

        var agent = new ParticipantAgent(emptyMap(), emptyMap());

        policyEngine.registerFunction("foo", Permission.class, (rule, context) -> Assertions.fail("Foo permission should be out of scope"));
        policyEngine.registerFunction("bar", Permission.class, (rule, context) -> rule.getAction().getType().equals(action.getType()));
        assertThat(policyEngine.evaluate("bar", policy, agent).succeeded()).isTrue();
    }

    @Test
    void validatePreValidator() {
        policyEngine.registerPreValidator(ALL_SCOPES, (policy, context) -> false);
        var policy = Policy.Builder.newInstance().build();
        var agent = new ParticipantAgent(emptyMap(), emptyMap());

        var result = policyEngine.evaluate(TEST_SCOPE, policy, agent);

        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void validatePostValidator() {
        policyEngine.registerPostValidator(ALL_SCOPES, (policy, context) -> false);
        var policy = Policy.Builder.newInstance().build();
        var agent = new ParticipantAgent(emptyMap(), emptyMap());

        var result = policyEngine.evaluate(TEST_SCOPE, policy, agent);

        assertThat(result.succeeded()).isFalse();
    }

    @BeforeEach
    void setUp() {
        policyEngine = new PolicyEngineImpl();
    }

    private Policy createTestPolicy() {
        var left = new LiteralExpression("foo");
        var right = new LiteralExpression("bar");
        var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();
        var prohibition = Prohibition.Builder.newInstance().constraint(constraint).build();
        return Policy.Builder.newInstance().prohibition(prohibition).build();
    }

}
