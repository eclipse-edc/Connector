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

class DutyTest {

    @Test
    void serializeDeserialize() throws JsonProcessingException {
        var mapper = new ObjectMapper();
        var serialized = mapper.writeValueAsString(Duty.Builder.newInstance().action(Action.Builder.newInstance().type("USE").build()).build());
        assertThat(mapper.readValue(serialized, Duty.class).getAction()).isNotNull();
    }
    
    @Test
    void withTarget() {
        var target = "target-id";
        var duty = Duty.Builder.newInstance()
                .uid("id")
                .assigner("assigner")
                .assignee("assignee")
                .action(Action.Builder.newInstance().type("DELETE").build())
                .constraint(AtomicConstraint.Builder.newInstance()
                        .leftExpression(new LiteralExpression("left"))
                        .operator(Operator.EQ)
                        .rightExpression(new LiteralExpression("right"))
                        .build())
                .consequence(Duty.Builder.newInstance().build())
                .build();
        
        var copy = duty.withTarget(target);
    
        assertThat(copy.getUid()).isEqualTo(duty.getUid());
        assertThat(copy.getAssigner()).isEqualTo(duty.getAssigner());
        assertThat(copy.getAssignee()).isEqualTo(duty.getAssignee());
        assertThat(copy.getAction()).isEqualTo(duty.getAction());
        assertThat(copy.getConstraints()).isEqualTo(duty.getConstraints());
        assertThat(copy.getConsequence().getTarget()).isEqualTo(target);
        assertThat(copy.getTarget()).isEqualTo(target);
    }
}
