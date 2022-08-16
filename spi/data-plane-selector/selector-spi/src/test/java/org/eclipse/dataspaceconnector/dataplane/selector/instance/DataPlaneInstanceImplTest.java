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

package org.eclipse.dataspaceconnector.dataplane.selector.instance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DataPlaneInstanceImplTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setup() {
        var tm = new TypeManager();
        tm.registerTypes(DataPlaneInstanceImpl.class);
        mapper = tm.getMapper();
    }

    @Test
    void verifySerialization() throws MalformedURLException, JsonProcessingException {
        var inst = DataPlaneInstanceImpl.Builder.newInstance()
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
                .contains("\"edctype\":\"dataspaceconnector:dataplaneinstance\"")
                .contains("url\":\"http://localhost:8234/some/path\"")
                .contains("\"turnCount\":7")
                .contains("\"someprop\":\"someval\"");

        var deserialized = mapper.readValue(json, DataPlaneInstanceImpl.class);
        assertThat(deserialized).usingRecursiveComparison().isEqualTo(inst);

        var deserializedItf = mapper.readValue(json, DataPlaneInstance.class);
        assertThat(deserializedItf).usingRecursiveComparison().isEqualTo(inst);
    }

    @Test
    void verifyCanHandle() throws MalformedURLException {
        var srcType1 = "srcType1";
        var srcType2 = "srcType1";
        var destType1 = "destType1";
        var destType2 = "destType2";

        var inst = DataPlaneInstanceImpl.Builder.newInstance()
                .id("test-id")
                .url(new URL("http://localhost:8234/some/path"))
                .allowedSourceType(srcType1)
                .allowedSourceType(srcType2)
                .allowedDestType(destType1)
                .allowedDestType(destType2)
                .build();

        assertThat(inst.canHandle(createAddress(srcType1), createAddress(destType1))).isTrue();
        assertThat(inst.canHandle(createAddress(srcType1), createAddress(destType2))).isTrue();
        assertThat(inst.canHandle(createAddress(srcType2), createAddress(destType2))).isTrue();
        assertThat(inst.canHandle(createAddress(srcType2), createAddress(destType1))).isTrue();
        assertThat(inst.canHandle(createAddress(srcType2), createAddress("notexist"))).isFalse();
        assertThat(inst.canHandle(createAddress("notexist"), createAddress(destType1))).isFalse();

    }

    private DataAddress createAddress(String type) {
        return DataAddress.Builder.newInstance().type(type).build();
    }
}