/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.asset.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AssetUpdateRequestDtoTest {
    private ObjectMapper objectMapper;


    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void verifySerDes() throws JsonProcessingException {
        var dto = AssetUpdateRequestDto.Builder.newInstance()
                .properties(Map.of("key1", "value1", "key2", "value2"))
                .build();

        var json = objectMapper.writeValueAsString(dto);
        assertThat(json).isNotNull();

        var deser = objectMapper.readValue(json, AssetUpdateRequestDto.class);

        assertThat(deser).usingRecursiveComparison().isEqualTo(dto);
    }
}

