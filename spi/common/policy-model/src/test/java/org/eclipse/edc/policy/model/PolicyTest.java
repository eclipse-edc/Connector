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
 *       Fraunhofer Institute for Software and Systems Engineering - added test
 *
 */

package org.eclipse.edc.policy.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serDes() throws JsonProcessingException {
        var target = "target-id";
        var permission = Permission.Builder.newInstance().action(Action.Builder.newInstance().type("use").build()).build();
        var prohibition = Prohibition.Builder.newInstance().action(Action.Builder.newInstance().type("modify").build()).build();
        var duty = Duty.Builder.newInstance().action(Action.Builder.newInstance().type("delete").build()).build();
        var policy = Policy.Builder.newInstance()
                .permission(permission)
                .prohibition(prohibition)
                .duty(duty)
                .assigner("assigner")
                .assignee("assignee")
                .inheritsFrom("inheritsFrom")
                .type(PolicyType.SET)
                .extensibleProperties(new HashMap<>())
                .target(target)
                .build();

        var deserialized = mapper.readValue(mapper.writeValueAsString(policy), Policy.class);

        assertThat(deserialized.getPermissions().size()).isEqualTo(policy.getPermissions().size());
        assertThat(deserialized.getAssigner()).isEqualTo(policy.getAssigner());
        assertThat(deserialized.getAssignee()).isEqualTo(policy.getAssignee());
        assertThat(deserialized.getInheritsFrom()).isEqualTo(policy.getInheritsFrom());
        assertThat(deserialized.getType()).isEqualTo(policy.getType());
        assertThat(deserialized.getExtensibleProperties()).isEqualTo(policy.getExtensibleProperties());
        assertThat(deserialized.getTarget()).isEqualTo(target);
    }

}
