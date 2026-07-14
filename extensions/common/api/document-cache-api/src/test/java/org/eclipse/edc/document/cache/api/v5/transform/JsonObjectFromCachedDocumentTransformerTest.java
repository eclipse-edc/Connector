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
import org.eclipse.edc.document.cache.spi.CachedDocument;
import org.eclipse.edc.document.cache.spi.PullStrategy;
import org.eclipse.edc.transform.TransformerContextImpl;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.document.cache.spi.CachedDocument.CACHED_DOCUMENT_CONTENT_IRI;
import static org.eclipse.edc.document.cache.spi.CachedDocument.CACHED_DOCUMENT_PULL_STRATEGY_IRI;
import static org.eclipse.edc.document.cache.spi.CachedDocument.CACHED_DOCUMENT_TYPE_IRI;
import static org.eclipse.edc.document.cache.spi.CachedDocument.CACHED_DOCUMENT_URL_IRI;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JsonObjectFromCachedDocumentTransformerTest {

    private final JsonObjectFromCachedDocumentTransformer transformer =
            new JsonObjectFromCachedDocumentTransformer(Json.createBuilderFactory(emptyMap()));
    private final TransformerContext context = new TransformerContextImpl(mock());

    @Test
    void shouldTransformToJson() {
        var cached = CachedDocument.Builder.newInstance()
                .id("id1")
                .url("https://example.com/context.jsonld")
                .content("{\"@context\":{\"foo\":\"https://example.com/ns/foo\"}}")
                .pullStrategy(PullStrategy.NEVER)
                .build();

        var result = transformer.transform(cached, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("id1");
        assertThat(result.getString(TYPE)).isEqualTo(CACHED_DOCUMENT_TYPE_IRI);
        assertThat(result.getString(CACHED_DOCUMENT_URL_IRI)).isEqualTo("https://example.com/context.jsonld");
        assertThat(result.getString(CACHED_DOCUMENT_PULL_STRATEGY_IRI)).isEqualTo("NEVER");

        var content = result.getJsonObject(CACHED_DOCUMENT_CONTENT_IRI);
        assertThat(content.getString(TYPE)).isEqualTo(JSON);
        assertThat(content.getJsonObject(VALUE).getJsonObject("@context").getString("foo"))
                .isEqualTo("https://example.com/ns/foo");
    }

    @Test
    void shouldTransformToJson_whenContentAbsent() {
        var cached = CachedDocument.Builder.newInstance()
                .id("id2")
                .url("https://example.com/context.jsonld")
                .pullStrategy(PullStrategy.IF_NOT_PRESENT)
                .build();

        var result = transformer.transform(cached, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(CACHED_DOCUMENT_PULL_STRATEGY_IRI)).isEqualTo("IF_NOT_PRESENT");
        assertThat(result.get(CACHED_DOCUMENT_CONTENT_IRI)).isNull();
    }

    @Test
    void shouldReportProblem_whenContentIsNotValidJson() {
        var transformerContext = mock(TransformerContext.class);
        var cached = CachedDocument.Builder.newInstance()
                .url("https://example.com/context.jsonld")
                .content("not-a-json-object")
                .build();

        var result = transformer.transform(cached, transformerContext);

        assertThat(result).isNotNull();
        assertThat(result.get(CACHED_DOCUMENT_CONTENT_IRI)).isNull();
        verify(transformerContext).reportProblem(anyString());
    }
}
