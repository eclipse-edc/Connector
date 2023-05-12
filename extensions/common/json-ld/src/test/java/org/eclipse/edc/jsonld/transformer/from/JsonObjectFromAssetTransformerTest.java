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

package org.eclipse.edc.jsonld.transformer.from;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.transformer.Payload;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.mockito.Mockito.mock;

class JsonObjectFromAssetTransformerTest {

    private static final String TEST_CONTENT_TYPE = "application/json";
    private static final String TEST_ASSET_ID = "test-asset-id";
    private static final String TEST_DESCRIPTION = "test-description";
    private static final String TEST_VERSION = "0.6.9";
    private static final String TEST_ASSET_NAME = "test-asset";
    private JsonObjectFromAssetTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromAssetTransformer(Json.createBuilderFactory(Map.of()), createObjectMapper());
    }

    @Test
    void transform_noCustomProperties() {
        var asset = createAssetBuilder()
                .build();

        var jsonObject = transformer.transform(asset, mock(TransformerContext.class));

        assertThat(jsonObject).isNotNull().hasSize(7);
        assertThat(jsonObject.getJsonString(ID).getString()).isEqualTo(TEST_ASSET_ID);
        assertThat(jsonObject.getJsonString(TYPE).getString()).isEqualTo(Asset.EDC_ASSET_TYPE);
        assertThat(jsonObject.getJsonString(EDC_NAMESPACE + "id").getString()).isEqualTo(TEST_ASSET_ID);
        assertThat(jsonObject.getJsonString(EDC_NAMESPACE + "contenttype").getString()).isEqualTo(TEST_CONTENT_TYPE);
        assertThat(jsonObject.getJsonString(EDC_NAMESPACE + "description").getString()).isEqualTo(TEST_DESCRIPTION);
        assertThat(jsonObject.getJsonString(EDC_NAMESPACE + "name").getString()).isEqualTo(TEST_ASSET_NAME);
        assertThat(jsonObject.getJsonString(EDC_NAMESPACE + "version").getString()).isEqualTo(TEST_VERSION);
    }

    @Test
    void transform_customProperties_simpleTypes() {
        var asset = createAssetBuilder()
                .property("some-key", "some-value")
                .build();

        var jsonObject = transformer.transform(asset, mock(TransformerContext.class));

        assertThat(jsonObject).isNotNull();
        assertThat(jsonObject.getJsonString("some-key").getString()).isEqualTo("some-value");
    }

    @Test
    void transform_customProperties_withExpandedNamespace() {
        var asset = createAssetBuilder()
                .property("https://foo.bar.org/schema/some-key", "some-value")
                .build();

        var jsonObject = transformer.transform(asset, mock(TransformerContext.class));

        assertThat(jsonObject).isNotNull();
        assertThat(jsonObject.getJsonString("https://foo.bar.org/schema/some-key").getString()).isEqualTo("some-value");
    }

    @Test
    void transform_customProperties_withCustomObject() {
        var asset = createAssetBuilder()
                .property("https://foo.bar.org/schema/payload", new Payload("foo-bar", 42))
                .build();

        var mock = mock(TransformerContext.class);
        var jsonObject = transformer.transform(asset, mock);

        assertThat(jsonObject).isNotNull();
        assertThat(jsonObject.getJsonObject("https://foo.bar.org/schema/payload")).isInstanceOf(JsonObject.class);
    }

    private Asset.Builder createAssetBuilder() {
        return Asset.Builder.newInstance()
                .id(TEST_ASSET_ID)
                .version(TEST_VERSION)
                .contentType(TEST_CONTENT_TYPE)
                .description(TEST_DESCRIPTION)
                .name(TEST_ASSET_NAME);
    }
}