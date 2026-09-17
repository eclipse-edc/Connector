/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.policy.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.json.JacksonTypeManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.policy.model.Operator.IS_PART_OF;

class OperatorTest {

    private final ObjectMapper mapper = new JacksonTypeManager().getMapper();

    @Test
    void shouldSerializeInAsIsPartOf() throws JsonProcessingException {
        var serialized = mapper.writeValueAsString(Operator.IN);

        assertThat(serialized).contains(IS_PART_OF.name());
    }

    @Test
    void shouldDeserializeInAsIsPartOf() throws JsonProcessingException {
        var serialized = "\"IN\"";

        var operator = mapper.readValue(serialized, Operator.class);

        assertThat(operator).isEqualTo(IS_PART_OF);
    }
}