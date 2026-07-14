/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.document.cache.api.v5.transform;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.document.cache.spi.CachedDocument;
import org.eclipse.edc.document.cache.spi.PullStrategy;
import org.eclipse.edc.jsonld.test.TestJsonLd;
import org.eclipse.edc.transform.TypeTransformerRegistryImpl;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.document.cache.spi.CachedDocument.CACHED_DOCUMENT_TYPE_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;

class JsonObjectToCachedDocumentTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private TypeTransformerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TypeTransformerRegistryImpl();
        registry.register(new JsonObjectToCachedDocumentTransformer());
    }

    @Test
    void shouldTransformToEntity() {
        var jsonObject = jsonFactory.createObjectBuilder()
                .add(CONTEXT, EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2)
                .add(TYPE, CACHED_DOCUMENT_TYPE_TERM)
                .add("url", "https://example.com/context.jsonld")
                .add("pullStrategy", "NEVER")
                .add("content", jsonFactory.createObjectBuilder()
                        .add("@context", jsonFactory.createObjectBuilder().add("foo", "https://example.com/ns/foo")))
                .build();

        var result = registry.transform(TestJsonLd.expand(jsonObject), CachedDocument.class);

        assertThat(result).isSucceeded().satisfies(cached -> {
            assertThat(cached.getUrl()).isEqualTo("https://example.com/context.jsonld");
            assertThat(cached.getPullStrategy()).isEqualTo(PullStrategy.NEVER);
            assertThat(cached.getContent()).contains("@context").contains("foo").contains("https://example.com/ns/foo");
        });
    }

    @Test
    void shouldGenerateId_whenNotProvided() {
        var jsonObject = jsonFactory.createObjectBuilder()
                .add(CONTEXT, EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2)
                .add(TYPE, CACHED_DOCUMENT_TYPE_TERM)
                .add("url", "https://example.com/context.jsonld")
                .build();

        var result = registry.transform(TestJsonLd.expand(jsonObject), CachedDocument.class);

        assertThat(result).isSucceeded().satisfies(cached -> assertThat(cached.getId()).isNotBlank());
    }

    @Test
    void shouldUseProvidedId() {
        var jsonObject = jsonFactory.createObjectBuilder()
                .add(CONTEXT, EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2)
                .add(ID, "https://example.com/ids/context-1")
                .add(TYPE, CACHED_DOCUMENT_TYPE_TERM)
                .add("url", "https://example.com/context.jsonld")
                .build();

        var result = registry.transform(TestJsonLd.expand(jsonObject), CachedDocument.class);

        assertThat(result).isSucceeded()
                .satisfies(cached -> assertThat(cached.getId()).isEqualTo("https://example.com/ids/context-1"));
    }

    @Test
    void shouldDefaultToIfNotPresent_whenContentAbsent() {
        var jsonObject = jsonFactory.createObjectBuilder()
                .add(CONTEXT, EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2)
                .add(TYPE, CACHED_DOCUMENT_TYPE_TERM)
                .add("url", "https://example.com/context.jsonld")
                .build();

        var result = registry.transform(TestJsonLd.expand(jsonObject), CachedDocument.class);

        assertThat(result).isSucceeded().satisfies(cached -> {
            assertThat(cached.getContent()).isNull();
            assertThat(cached.getPullStrategy()).isEqualTo(PullStrategy.IF_NOT_PRESENT);
        });
    }

    @Test
    void shouldFail_whenPullStrategyInvalid() {
        var jsonObject = jsonFactory.createObjectBuilder()
                .add(CONTEXT, EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2)
                .add(TYPE, CACHED_DOCUMENT_TYPE_TERM)
                .add("url", "https://example.com/context.jsonld")
                .add("pullStrategy", "sometimes")
                .build();

        var result = registry.transform(TestJsonLd.expand(jsonObject), CachedDocument.class);

        assertThat(result).isFailed();
    }

    @Test
    void shouldFail_whenUrlMissing() {
        var jsonObject = jsonFactory.createObjectBuilder()
                .add(CONTEXT, EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2)
                .add(TYPE, CACHED_DOCUMENT_TYPE_TERM)
                .add("pullStrategy", "NEVER")
                .build();

        var result = registry.transform(TestJsonLd.expand(jsonObject), CachedDocument.class);

        assertThat(result).isFailed();
    }
}
