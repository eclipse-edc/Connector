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
import org.eclipse.edc.api.model.DataAddressDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

public class AssetEntryNewDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = createObjectMapper();
    }

    @Test
    void verifySerialization() throws JsonProcessingException {

        var assetDto = createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "Asset")
                .add(ID, "test-asset-id")
                .add(CONTEXT, createObjectBuilder().add(VOCAB, EDC_NAMESPACE).build()) //default namespace
                .add(EDC_NAMESPACE + "properties", createObjectBuilder()
                        .add(EDC_NAMESPACE + "name", "test-name"))
                .build();
        var dataAddress = DataAddressDto.Builder.newInstance().properties(Collections.singletonMap("type", "test-type")).build();
        var assetEntryDto = AssetEntryNewDto.Builder.newInstance().asset(assetDto).dataAddress(dataAddress).build();

        var str = objectMapper.writeValueAsString(assetEntryDto);

        assertThat(str).isNotNull();

        var deserialized = objectMapper.readValue(str, AssetEntryNewDto.class);
        assertThat(deserialized).usingRecursiveComparison().isEqualTo(assetEntryDto);
    }
}
