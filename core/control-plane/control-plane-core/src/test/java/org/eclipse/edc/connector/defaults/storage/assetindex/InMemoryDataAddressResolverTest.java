/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.edc.connector.defaults.storage.assetindex;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryDataAddressResolverTest {
    private InMemoryAssetIndex resolver;

    @BeforeEach
    void setUp() {
        resolver = new InMemoryAssetIndex();
    }

    @Test
    void resolveForAsset() {
        var id = UUID.randomUUID().toString();
        var address = createDataAddress();
        var testAsset = createAssetBuilder("foobar", id).dataAddress(address).build();
        resolver.create(testAsset);

        assertThat(resolver.resolveForAsset(testAsset.getId())).isEqualTo(address);
    }

    @Test
    void resolveForAsset_assetNull_raisesException() {
        var id = UUID.randomUUID().toString();
        var address = createDataAddress();
        var testAsset = createAssetBuilder("foobar", id).dataAddress(address).build();
        resolver.create(testAsset);

        assertThatThrownBy(() -> resolver.resolveForAsset(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void resolveForAsset_whenAssetDeleted_raisesException() {
        var address = createDataAddress();
        var testAsset = createAssetBuilder("foobar", UUID.randomUUID().toString()).dataAddress(address).build();
        resolver.create(testAsset);
        resolver.deleteById(testAsset.getId());

        assertThat(resolver.resolveForAsset(testAsset.getId())).isNull();
    }

    private static Asset.Builder createAssetBuilder(String name, String id) {
        return Asset.Builder.newInstance().id(id).name(name).version("1").contentType("type");
    }

    private DataAddress createDataAddress() {
        return DataAddress.Builder.newInstance()
                .keyName("test-keyname")
                .type("type")
                .build();
    }
}
