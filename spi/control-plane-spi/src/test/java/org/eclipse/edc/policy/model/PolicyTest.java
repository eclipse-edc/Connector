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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - added test
 *
 */

package org.eclipse.edc.policy.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyTest {

    @Test
    void serializeDeserialize() throws JsonProcessingException {
        var mapper = new ObjectMapper();

        var permission = Permission.Builder.newInstance().action(Action.Builder.newInstance().type("use").build()).build();
        var policy = Policy.Builder.newInstance().permission(permission).build();

        var serialized = mapper.writeValueAsString(policy);
        assertThat(mapper.readValue(serialized, Policy.class).getPermissions()).isNotEmpty();
    }

    @Test
    void withTarget() {
        var target = "target-id";
        var permission = Permission.Builder.newInstance().action(Action.Builder.newInstance().type("use").build()).build();
        var prohibition = Prohibition.Builder.newInstance().action(Action.Builder.newInstance().type("MODIFY").build()).build();
        var duty = Duty.Builder.newInstance().action(Action.Builder.newInstance().type("DELETE").build()).build();
        var policy = Policy.Builder.newInstance()
                .permission(permission)
                .prohibition(prohibition)
                .duty(duty)
                .assigner("assigner")
                .assignee("assignee")
                .inheritsFrom("inheritsFroms")
                .type(PolicyType.SET)
                .extensibleProperties(new HashMap<>())
                .build();

        var copy = policy.withTarget(target);

        assertThat(copy.getPermissions().size()).isEqualTo(policy.getPermissions().size());
        assertThat(copy.getAssigner()).isEqualTo(policy.getAssigner());
        assertThat(copy.getAssignee()).isEqualTo(policy.getAssignee());
        assertThat(copy.getInheritsFrom()).isEqualTo(policy.getInheritsFrom());
        assertThat(copy.getType()).isEqualTo(policy.getType());
        assertThat(copy.getExtensibleProperties()).isEqualTo(policy.getExtensibleProperties());
        assertThat(copy.getTarget()).isEqualTo(target);
    }

}
