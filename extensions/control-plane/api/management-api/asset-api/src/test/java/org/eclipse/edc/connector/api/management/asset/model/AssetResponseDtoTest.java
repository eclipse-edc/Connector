/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.api.management.asset.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class AssetResponseDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new TypeManager().getMapper();
    }

    @Test
    void verifySerialization() throws JsonProcessingException {
        var assetDto = AssetResponseDto.Builder.newInstance()
                .properties(Collections.singletonMap("Asset-1", ""))
                .privateProperties(Collections.singletonMap("pAsset-1", ""))
                .build();

        var str = objectMapper.writeValueAsString(assetDto);

        assertThat(str).isNotNull();

        var deserialized = objectMapper.readValue(str, AssetResponseDto.class);
        assertThat(deserialized).usingRecursiveComparison().isEqualTo(assetDto);
    }
}
