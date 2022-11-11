/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.api.management.policy.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyDefinitionResponseDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void verifySerialization() throws JsonProcessingException {
        var dto = PolicyDefinitionResponseDto.Builder.newInstance()
                .id("test-id")
                .policy(Policy.Builder.newInstance().build())
                .build();

        var str = objectMapper.writeValueAsString(dto);

        assertThat(str).isNotNull();

        var deserialized = objectMapper.readValue(str, PolicyDefinitionResponseDto.class);
        assertThat(deserialized).usingRecursiveComparison().isEqualTo(dto);
    }
}
