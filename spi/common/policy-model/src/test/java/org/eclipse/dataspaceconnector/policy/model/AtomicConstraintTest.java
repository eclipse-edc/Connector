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

package org.eclipse.dataspaceconnector.policy.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AtomicConstraintTest {

    @Test
    void serializeDeserialize() throws JsonProcessingException {
        var mapper = new ObjectMapper();
        mapper.registerSubtypes(LiteralExpression.class);

        var serialized = mapper.writeValueAsString(AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression("left"))
                .rightExpression(new LiteralExpression("right"))
                .build());
        var atomicConstraint = mapper.readValue(serialized, AtomicConstraint.class);

        assertThat(atomicConstraint).isNotNull();
        assertThat(atomicConstraint.getLeftExpression()).isNotNull();
        assertThat(atomicConstraint.getRightExpression()).isNotNull();
    }

}
