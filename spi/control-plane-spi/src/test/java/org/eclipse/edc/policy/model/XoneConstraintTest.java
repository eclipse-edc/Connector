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

package org.eclipse.edc.policy.model;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class XoneConstraintTest {

    @Test
    void serializeDeserialize() throws JacksonException {
        var mapper = JsonMapper.builder().registerSubtypes(AtomicConstraint.class, LiteralExpression.class).build();

        var constraint = AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression("left"))
                .rightExpression(new LiteralExpression("bar"))
                .build();
        var serialized = mapper.writeValueAsString(XoneConstraint.Builder.newInstance().constraint(constraint).build());
        assertThat(mapper.readValue(serialized, XoneConstraint.class).getConstraints()).isNotEmpty();
    }

}
