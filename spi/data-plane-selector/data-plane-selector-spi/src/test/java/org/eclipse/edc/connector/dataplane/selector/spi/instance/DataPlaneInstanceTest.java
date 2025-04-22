/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.selector.spi.instance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DataPlaneInstanceTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setup() {
        var tm = new JacksonTypeManager();
        tm.registerTypes(DataPlaneInstance.class);
        mapper = tm.getMapper();
    }

    @Test
    void verifySerialization() throws MalformedURLException, JsonProcessingException {
        var inst = DataPlaneInstance.Builder.newInstance()
                .id("test-id")
                .turnCount(7)
                .lastActive(Instant.now().toEpochMilli())
                .url(new URL("http://localhost:8234/some/path"))
                .property("someprop", "someval")
                .allowedSourceType("allowedSrc1")
                .allowedSourceType("allowedSrc2")
                .allowedDestType("allowedDest1")
                .allowedDestType("allowedDest2")
                .build();

        var json = mapper.writeValueAsString(inst);

        assertThat(json).isNotNull()
                .contains("url\":\"http://localhost:8234/some/path\"")
                .contains("\"turnCount\":7")
                .contains("\"someprop\":\"someval\"");

        var deserialized = mapper.readValue(json, DataPlaneInstance.class).copy();
        assertThat(deserialized).usingRecursiveComparison().isEqualTo(inst);
    }

    @Test
    void verifyCanHandle_withTransferType() throws MalformedURLException {
        var srcType1 = "srcType1";
        var srcType2 = "srcType1";
        var transferType1 = "customTransferType1";
        var transferType2 = "customTransferType2";

        var inst = DataPlaneInstance.Builder.newInstance()
                .id("test-id")
                .url(new URL("http://localhost:8234/some/path"))
                .allowedSourceType(srcType1)
                .allowedSourceType(srcType2)
                .allowedTransferType(transferType1)
                .allowedTransferType(transferType2)
                .build();

        assertThat(inst.canHandle(createAddress(srcType1), transferType1)).isTrue();
        assertThat(inst.canHandle(createAddress(srcType1), transferType1)).isTrue();
        assertThat(inst.canHandle(createAddress(srcType2), transferType2)).isTrue();
        assertThat(inst.canHandle(createAddress(srcType2), transferType2)).isTrue();
        assertThat(inst.canHandle(createAddress(srcType1), "notexist")).isFalse();
    }

    @Test
    void verifyCanProvisionDestination() throws MalformedURLException {
        var provisionType = "provisionType";
        var inst = DataPlaneInstance.Builder.newInstance()
                .id("test-id")
                .url(new URL("http://localhost:8234/some/path"))
                .allowedSourceType("srcType")
                .allowedTransferType("destType")
                .destinationProvisionTypes(Set.of("provisionType"))
                .build();

        assertThat(inst.canProvisionDestination(null)).isFalse();
        assertThat(inst.canProvisionDestination(createAddress(provisionType))).isTrue();
        assertThat(inst.canProvisionDestination(createAddress("anotherType"))).isFalse();
    }

    private DataAddress createAddress(String type) {
        return DataAddress.Builder.newInstance().type(type).build();
    }
}
