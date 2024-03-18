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

package org.eclipse.edc.connector.core.store;

import org.eclipse.edc.spi.query.PropertyLookup;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetPropertyLookupTest {

    private final PropertyLookup propertyLookup = new AssetPropertyLookup();

    @Test
    void shouldGetProperty() {
        var asset = Asset.Builder.newInstance()
                .name("test-asset")
                .build();

        var property = propertyLookup.getProperty(Asset.PROPERTY_NAME, asset);

        assertThat(property).isEqualTo("test-asset");
    }

    @Test
    void shouldGetCustomProperty() {
        var asset = Asset.Builder.newInstance()
                .name("test-asset")
                .version("6.9")
                .property("test-property", "some-value")
                .build();

        var property = propertyLookup.getProperty("test-property", asset);

        assertThat(property).isEqualTo("some-value");
    }

    @Test
    void shouldGetPrivateProperty() {
        var asset = Asset.Builder.newInstance()
                .name("test-asset")
                .property("test-property", "some-value")
                .privateProperty("test-private-property", "somePrivateValue")
                .build();

        var property = propertyLookup.getProperty("test-private-property", asset);

        assertThat(property).isEqualTo("somePrivateValue");
    }

    @Test
    void shouldReturnNull_whenPropertyDoesNotExist() {
        var asset = Asset.Builder.newInstance().build();

        var property = propertyLookup.getProperty("not-existent", asset);

        assertThat(property).isNull();
    }

    @Test
    void shouldReturnNull_whenObjectIsNotAnAsset() {
        var property = propertyLookup.getProperty("not-existent", "not-an-asset");

        assertThat(property).isNull();
    }
}
