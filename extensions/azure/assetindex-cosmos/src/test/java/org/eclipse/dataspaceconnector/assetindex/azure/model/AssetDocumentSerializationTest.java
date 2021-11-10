/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.assetindex.azure.model;

import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetDocumentSerializationTest {

    private TypeManager typeManager;

    @BeforeEach
    void setup() {
        typeManager = new TypeManager();
        typeManager.registerTypes(AssetDocument.class, Asset.class);
    }

    @Test
    void testSerialization() {
        var asset = createAsset();

        var document = new AssetDocument(asset, "partitionkey-test");

        String s = typeManager.writeValueAsString(document);

        assertThat(s).isNotNull()
                .contains("\"partitionKey\":\"partitionkey-test\"")
                .contains("\"asset:prop:id\":\"id-test\"")
                .contains("\"asset:prop:name\":\"node-test\"")
                .contains("\"asset:prop:version\":\"123\"")
                .contains("\"asset:prop:contenttype\":\"application/json\"")
                .contains("\"foo\":\"bar\"")
                .contains("\"sanitizedProperties\":")
                .contains("\"asset_prop_id\":\"id-test\"")
                .contains("\"asset_prop_name\":\"node-test\"");
    }

    @Test
    void testDeserialization() {
        var asset = createAsset();

        var document = new AssetDocument(asset, "partitionkey-test");
        String json = typeManager.writeValueAsString(document);

        var deserialized = typeManager.readValue(json, AssetDocument.class);
        assertThat(deserialized.getWrappedInstance()).usingRecursiveComparison().isEqualTo(document.getWrappedInstance());
    }

    private static Asset createAsset() {
        return Asset.Builder.newInstance()
                .id("id-test")
                .name("node-test")
                .contentType("application/json")
                .version("123")
                .property("foo", "bar")
                .build();
    }
}
