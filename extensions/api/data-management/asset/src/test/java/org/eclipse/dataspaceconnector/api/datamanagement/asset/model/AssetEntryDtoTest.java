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

package org.eclipse.dataspaceconnector.api.datamanagement.asset.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class AssetEntryDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void verifySerialization() throws JsonProcessingException {

        var assetDto = AssetDto.Builder.newInstance().properties(Collections.singletonMap("Asset-1", "")).build();
        var dataAddress = DataAddressDto.Builder.newInstance().properties(Collections.singletonMap("asset-1", "/localhost")).build();

        var assetEntryDto = AssetEntryDto.Builder.newInstance().asset(assetDto).dataAddress(dataAddress).build();

        var str = objectMapper.writeValueAsString(assetEntryDto);

        assertThat(str).isNotNull();

        var deserialized = objectMapper.readValue(str, AssetEntryDto.class);
        assertThat(deserialized).usingRecursiveComparison().isEqualTo(assetEntryDto);
    }
}
