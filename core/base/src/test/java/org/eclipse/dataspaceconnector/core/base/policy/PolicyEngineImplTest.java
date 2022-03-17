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

import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.policy.model.Operator.EQ;

class PolicyEngineImplTest {
    private PolicyEngineImpl policyEngine;

    @Test
    void validateEmptyPolicy() {
        var agent = new ParticipantAgent(emptyMap(), emptyMap());

        var emptyPolicy = Policy.Builder.newInstance().build();

        // No explicit rule specified, policy should evaluate to true
        var result = policyEngine.evaluate(emptyPolicy, agent);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validateUnsatisfiedDuty() {
        policyEngine.registerFunction(Duty.class, "foo", (op, rv, duty, context) -> false);
        var agent = new ParticipantAgent(emptyMap(), emptyMap());

        var left = new LiteralExpression("foo");
        var right = new LiteralExpression("bar");
        var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();
        var duty = Duty.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().duty(duty).build();

        // The duty is not satisfied, so the policy should evaluate to false
        assertThat(policyEngine.evaluate(policy, agent).succeeded()).isFalse();
    }

    @Test
    void validateUngrantedPermission() {
        policyEngine.registerFunction(Permission.class, "foo", (op, rv, duty, context) -> false);
        var agent = new ParticipantAgent(emptyMap(), emptyMap());

        var left = new LiteralExpression("foo");
        var right = new LiteralExpression("bar");
        var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();
        var permission = Permission.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().permission(permission).build();

        // The permission is not granted, so the policy should evaluate to false
        var result = policyEngine.evaluate(policy, agent);

        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void validateTriggeredProhibition() {
        policyEngine.registerFunction(Prohibition.class, "foo", (op, rv, duty, context) -> true);
        var agent = new ParticipantAgent(emptyMap(), emptyMap());

        var left = new LiteralExpression("foo");
        var right = new LiteralExpression("bar");
        var constraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(EQ).rightExpression(right).build();
        var prohibition = Prohibition.Builder.newInstance().constraint(constraint).build();
        var policy = Policy.Builder.newInstance().prohibition(prohibition).build();

        // The prohibition is triggered (it is true), so the policy should evaluate to false
        var result = policyEngine.evaluate(policy, agent);

        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void validatePreValidator() {
        policyEngine.registerPreValidator((policy, context) -> false);
        var policy = Policy.Builder.newInstance().build();
        var agent = new ParticipantAgent(emptyMap(), emptyMap());

        var result = policyEngine.evaluate(policy, agent);

        assertThat(result.succeeded()).isFalse();
    }

    @Test
    void validatePostValidator() {
        policyEngine.registerPostValidator((policy, context) -> false);
        var policy = Policy.Builder.newInstance().build();
        var agent = new ParticipantAgent(emptyMap(), emptyMap());

        var result = policyEngine.evaluate(policy, agent);

        assertThat(result.succeeded()).isFalse();
    }

    @BeforeEach
    void setUp() {
        policyEngine = new PolicyEngineImpl();
    }
}
