/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.spi.types.domain.asset;

import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;

class AssetTest {

    private TypeManager typeManager;

    @BeforeEach
    void setUp() {
        typeManager = new TypeManager();
    }

    @Test
    void verifySerialization() {
        var asset = Asset.Builder.newInstance().id("abcd123")
                .contentType("application/json")
                .version("1.0")
                .name("testasset")
                .property("some-critical.value", 21347)
                .build();

        var json = typeManager.writeValueAsString(asset);

        assertThat(json).isNotNull().contains("abcd123")
                .contains("application/json")
                .contains("testasset")
                .contains("some-critical.value")
                .contains("21347")
                .contains("\"id\":\"abcd123\"")
                .contains("createdAt")
                .contains("1.0");
    }

    @Test
    void verifyDeserialization() {
        var json = getResourceFileContentAsString("serialized_asset.json");
        var asset = typeManager.readValue(json, Asset.class);

        assertThat(asset).isNotNull();
        assertThat(asset.getId()).isEqualTo("abcd123");
        assertThat(asset.getContentType()).isEqualTo("application/json");
        assertThat(asset.getName()).isNull();
        assertThat(asset.getProperty("numberVal")).isInstanceOf(Integer.class).isEqualTo(42069);
        assertThat(asset.getCreatedAt()).isNotEqualTo(0);
        assertThat(asset.getProperties()).hasSize(5);

    }

    @Test
    void getProperty_whenNotPresent_shouldReturnNull() {
        var asset = Asset.Builder.newInstance().build();
        assertThat(asset.getProperty("notexist")).isNull();
    }

    @Test
    void getNamedProperty_whenNotPresent_shouldReturnNull() {
        var asset = Asset.Builder.newInstance().build();
        assertThat(asset.getName()).isNull();
        assertThat(asset.getVersion()).isNull();
    }
}
