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

package org.eclipse.dataspaceconnector.policy.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionTest {

    @Test
    void serializeDeserialize() throws JsonProcessingException {
        var mapper = new ObjectMapper();
        var serialized = mapper.writeValueAsString(Permission.Builder.newInstance().action(Action.Builder.newInstance().type("USE").build()).build());
        assertThat(mapper.readValue(serialized, Permission.class).getAction()).isNotNull();
    }
    
    @Test
    void withTarget() {
        var target = "target-id";
        var permission = Permission.Builder.newInstance()
                .uid("id")
                .assigner("assigner")
                .assignee("assignee")
                .action(Action.Builder.newInstance().type("USE").build())
                .constraint(AtomicConstraint.Builder.newInstance()
                        .leftExpression(new LiteralExpression("left"))
                        .operator(Operator.EQ)
                        .rightExpression(new LiteralExpression("right"))
                        .build())
                .duty(Duty.Builder.newInstance().build())
                .build();
        
        var copy = permission.withTarget(target);
        
        assertThat(copy.getUid()).isEqualTo(permission.getUid());
        assertThat(copy.getAssigner()).isEqualTo(permission.getAssigner());
        assertThat(copy.getAssignee()).isEqualTo(permission.getAssignee());
        assertThat(copy.getAction()).isEqualTo(permission.getAction());
        assertThat(copy.getConstraints()).isEqualTo(permission.getConstraints());
        assertThat(copy.getDuties().size()).isEqualTo(permission.getDuties().size());
        copy.getDuties().forEach(d -> assertThat(d.getTarget()).isEqualTo(target));
        assertThat(copy.getTarget()).isEqualTo(target);
    }

}
