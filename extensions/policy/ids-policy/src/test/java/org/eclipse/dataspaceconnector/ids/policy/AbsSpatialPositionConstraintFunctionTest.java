/*
 *  Copyright (c) 2021 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.policy;

import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.policy.model.Operator.EQ;
import static org.eclipse.dataspaceconnector.policy.model.Operator.GT;
import static org.eclipse.dataspaceconnector.policy.model.Operator.IN;
import static org.eclipse.dataspaceconnector.policy.model.Operator.NEQ;

class AbsSpatialPositionConstraintFunctionTest {

    private final AbsSpatialPositionConstraintFunction constraintFunction = new AbsSpatialPositionConstraintFunction();

    @Test
    void shouldVerifyEqConstraint() {
        var euAgent = new ParticipantAgent(Map.of("region", "eu"), emptyMap());
        var permission = dummyPermission();

        boolean result = constraintFunction.evaluate(EQ, "eu", permission, new MockPolicyContext(euAgent));

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotVerifyEqConstraint() {
        var euAgent = new ParticipantAgent(Map.of("region", "eu"), emptyMap());
        var permission = dummyPermission();

        boolean result = constraintFunction.evaluate(EQ, "us", permission, new MockPolicyContext(euAgent));

        assertThat(result).isFalse();
    }

    @Test
    void shouldVerifyInConstraint() {
        var euAgent = new ParticipantAgent(Map.of("region", "eu"), emptyMap());

        boolean result = constraintFunction.evaluate(IN, List.of("eu"), dummyPermission(), new MockPolicyContext(euAgent));

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotVerifyInConstraint() {
        var euAgent = new ParticipantAgent(Map.of("region", "eu"), emptyMap());

        boolean result = constraintFunction.evaluate(IN, List.of("us"), dummyPermission(), new MockPolicyContext(euAgent));

        assertThat(result).isFalse();
    }

    @Test
    void shouldVerifyNotEqConstraint() {
        var euAgent = new ParticipantAgent(Map.of("region", "eu"), emptyMap());

        boolean result = constraintFunction.evaluate(NEQ, "us", dummyPermission(), new MockPolicyContext(euAgent));

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotVerifyNotEqConstraint() {
        var euAgent = new ParticipantAgent(Map.of("region", "eu"), emptyMap());

        boolean result = constraintFunction.evaluate(NEQ, "eu", dummyPermission(), new MockPolicyContext(euAgent));

        assertThat(result).isFalse();
    }

    @Test
    void shouldVerifyGreatherThanConstraint() {
        var euAgent = new ParticipantAgent(Map.of("region", "eu"), emptyMap());

        boolean result = constraintFunction.evaluate(GT, "eu", dummyPermission(), new MockPolicyContext(euAgent));

        assertThat(result).isFalse();
    }

    private Permission dummyPermission() {
        return Permission.Builder.newInstance().build();
    }

}
