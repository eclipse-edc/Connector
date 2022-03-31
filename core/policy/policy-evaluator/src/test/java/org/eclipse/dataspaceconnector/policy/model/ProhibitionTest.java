/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.policy.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProhibitionTest {
    
    @Test
    void withTarget() {
        var target = "target-id";
        var prohibition = Prohibition.Builder.newInstance()
                .uid("id")
                .assigner("assigner")
                .assignee("assignee")
                .action(Action.Builder.newInstance().type("DELETE").build())
                .constraint(AtomicConstraint.Builder.newInstance()
                        .leftExpression(new LiteralExpression("left"))
                        .operator(Operator.EQ)
                        .rightExpression(new LiteralExpression("right"))
                        .build())
                .build();
        
        var copy = prohibition.withTarget(target);
    
        assertThat(copy.getUid()).isEqualTo(prohibition.getUid());
        assertThat(copy.getAssigner()).isEqualTo(prohibition.getAssigner());
        assertThat(copy.getAssignee()).isEqualTo(prohibition.getAssignee());
        assertThat(copy.getAction()).isEqualTo(prohibition.getAction());
        assertThat(copy.getConstraints()).isEqualTo(prohibition.getConstraints());
        assertThat(copy.getTarget()).isEqualTo(target);
    }
    
}
