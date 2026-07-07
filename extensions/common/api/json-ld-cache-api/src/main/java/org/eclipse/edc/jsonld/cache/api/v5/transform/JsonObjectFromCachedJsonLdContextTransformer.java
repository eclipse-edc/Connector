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

package org.eclipse.edc.jsonld.cache.api.v5.transform;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.cache.spi.CachedJsonLdContext;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.StringReader;

import static org.eclipse.edc.jsonld.cache.spi.CachedJsonLdContext.CACHED_JSON_LD_CONTEXT_CONTENT_IRI;
import static org.eclipse.edc.jsonld.cache.spi.CachedJsonLdContext.CACHED_JSON_LD_CONTEXT_PULL_STRATEGY_IRI;
import static org.eclipse.edc.jsonld.cache.spi.CachedJsonLdContext.CACHED_JSON_LD_CONTEXT_TYPE_IRI;
import static org.eclipse.edc.jsonld.cache.spi.CachedJsonLdContext.CACHED_JSON_LD_CONTEXT_UPDATED_AT_IRI;
import static org.eclipse.edc.jsonld.cache.spi.CachedJsonLdContext.CACHED_JSON_LD_CONTEXT_URL_IRI;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;

/**
 * Renders a {@link CachedJsonLdContext} as an (expanded) JSON-LD object.
 * <p>
 * The {@code content} property is emitted as a {@code @json} literal so the arbitrary JSON-LD context document
 * it carries is preserved verbatim through compaction instead of being expanded as a nested node.
 */
public class JsonObjectFromCachedJsonLdContextTransformer extends AbstractJsonLdTransformer<CachedJsonLdContext, JsonObject> {

    private final JsonBuilderFactory factory;

    public JsonObjectFromCachedJsonLdContextTransformer(JsonBuilderFactory factory) {
        super(CachedJsonLdContext.class, JsonObject.class);
        this.factory = factory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull CachedJsonLdContext context, @NotNull TransformerContext transformerContext) {
        var builder = factory.createObjectBuilder()
                .add(ID, context.getId())
                .add(TYPE, CACHED_JSON_LD_CONTEXT_TYPE_IRI)
                .add(CACHED_JSON_LD_CONTEXT_URL_IRI, context.getUrl())
                .add(CACHED_JSON_LD_CONTEXT_PULL_STRATEGY_IRI, context.getPullStrategy().value())
                .add(CACHED_JSON_LD_CONTEXT_UPDATED_AT_IRI, context.getUpdatedAt());

        if (context.getContent() != null) {
            try (var reader = Json.createReader(new StringReader(context.getContent()))) {
                builder.add(CACHED_JSON_LD_CONTEXT_CONTENT_IRI, factory.createObjectBuilder()
                        .add(VALUE, reader.readObject())
                        .add(TYPE, JSON));
            } catch (Exception e) {
                transformerContext.reportProblem("Cannot render 'content' of cached JSON-LD context '%s': %s"
                        .formatted(context.getUrl(), e.getMessage()));
            }
        }

        return builder.build();
    }
}
