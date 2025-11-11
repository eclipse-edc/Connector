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

package org.eclipse.edc.connector.controlplane.asset.spi.domain;

import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

class AssetTest {

    private final TypeManager typeManager = new JacksonTypeManager();

    @Test
    void verifySerialization() {
        var asset = Asset.Builder.newInstance().id("abcd123")
                .property("contenttype", "application/json")
                .property("version", "1.0")
                .property("name", "testasset")
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
        var json = TestUtils.getResourceFileContentAsString("serialized_asset.json");
        var asset = typeManager.readValue(json, Asset.class);

        assertThat(asset).isNotNull();
        assertThat(asset.getId()).isEqualTo("abcd123");
        assertThat(asset.getProperty(EDC_NAMESPACE + "contenttype")).isEqualTo("application/json");
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
    void isCatalog_whenNotPresent_shouldReturnFalse() {
        var asset = Asset.Builder.newInstance().build();
        assertThat(asset.isCatalog()).isFalse();
    }

    @Test
    void isCatalog_whenFalse_shouldReturnFalse() {
        var asset = Asset.Builder.newInstance().property(Asset.PROPERTY_IS_CATALOG, "false").build();
        assertThat(asset.isCatalog()).isFalse();

        var asset2 = Asset.Builder.newInstance().property(Asset.PROPERTY_IS_CATALOG, false).build();
        assertThat(asset2.isCatalog()).isFalse();
    }

    @Test
    void isCatalog_whenTrue_shouldReturnTrue() {
        var asset = Asset.Builder.newInstance().property(Asset.PROPERTY_IS_CATALOG, "true").build();
        assertThat(asset.isCatalog()).isTrue();

        var asset2 = Asset.Builder.newInstance().property(Asset.PROPERTY_IS_CATALOG, true).build();
        assertThat(asset2.isCatalog()).isTrue();
    }

    @Test
    void isCatalog_whenInvalidValid_shoudReturnFalse() {
        var asset = Asset.Builder.newInstance().property(Asset.PROPERTY_IS_CATALOG, "foobar").build();
        assertThat(asset.isCatalog()).isFalse();
    }
}
