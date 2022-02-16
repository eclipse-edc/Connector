/*
 * Copyright (c) 2022 Diego Gomez
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *   Diego Gomez - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.datamanagement.asset.model;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DataAddressTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void verifySerialization() throws JsonProcessingException {
        var dataAddress = DataAddress.Builder.newInstance().properties(Collections.singletonMap("asset-1", "/localhost")).build();

        var str = objectMapper.writeValueAsString(dataAddress);

        assertThat(str).isNotNull();

        var deserialized = objectMapper.readValue(str, DataAddress.class);
        assertThat(deserialized).usingRecursiveComparison().isEqualTo(dataAddress);
    }
}
