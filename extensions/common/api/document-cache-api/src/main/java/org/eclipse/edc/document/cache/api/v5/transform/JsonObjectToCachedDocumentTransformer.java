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

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.document.cache.spi.CachedDocument;
import org.eclipse.edc.document.cache.spi.CachedDocumentType;
import org.eclipse.edc.document.cache.spi.PullStrategy;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.document.cache.spi.CachedDocument.CACHED_DOCUMENT_CONTENT_IRI;
import static org.eclipse.edc.document.cache.spi.CachedDocument.CACHED_DOCUMENT_DOCUMENT_TYPE_IRI;
import static org.eclipse.edc.document.cache.spi.CachedDocument.CACHED_DOCUMENT_PULL_STRATEGY_IRI;
import static org.eclipse.edc.document.cache.spi.CachedDocument.CACHED_DOCUMENT_URL_IRI;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;

/**
 * Transforms the (expanded) request body of the document cache API into a {@link CachedDocument}.
 * <p>
 * The {@code content} property is declared as a {@code @json} literal in the management JSON-LD context, so the
 * arbitrary JSON document it carries survives expansion untouched and is read back from its
 * {@code @value}.
 */
public class JsonObjectToCachedDocumentTransformer extends AbstractJsonLdTransformer<JsonObject, CachedDocument> {

    public JsonObjectToCachedDocumentTransformer() {
        super(JsonObject.class, CachedDocument.class);
    }

    @Override
    public @Nullable CachedDocument transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var url = transformString(object.get(CACHED_DOCUMENT_URL_IRI), context);
        if (url == null) {
            context.reportProblem("Property 'url' is mandatory");
            return null;
        }

        var builder = CachedDocument.Builder.newInstance().url(url);

        var id = nodeId(object);
        if (id != null) {
            builder.id(id);
        }

        var documentType = transformString(object.get(CACHED_DOCUMENT_DOCUMENT_TYPE_IRI), context);
        if (documentType != null) {
            try {
                builder.type(CachedDocumentType.fromValue(documentType));
            } catch (IllegalArgumentException e) {
                context.reportProblem(e.getMessage());
                return null;
            }
        }

        var pullStrategy = transformString(object.get(CACHED_DOCUMENT_PULL_STRATEGY_IRI), context);
        if (pullStrategy != null) {
            try {
                builder.pullStrategy(PullStrategy.fromValue(pullStrategy));
            } catch (IllegalArgumentException e) {
                context.reportProblem(e.getMessage());
                return null;
            }
        }

        var content = readJsonContent(object.get(CACHED_DOCUMENT_CONTENT_IRI));
        if (content != null) {
            builder.content(content.toString());
        }

        return builder.build();
    }

    /**
     * Reads the raw JSON object carried by an expanded {@code @json} value, i.e. the {@code @value} of the
     * (single-element) expansion array.
     */
    private @Nullable JsonObject readJsonContent(@Nullable JsonValue value) {
        if (value instanceof JsonArray array && !array.isEmpty()) {
            value = array.get(0);
        }
        if (value instanceof JsonObject object && object.get(VALUE) instanceof JsonObject raw) {
            return raw;
        }
        return null;
    }
}
