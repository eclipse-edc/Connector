/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.catalog.transform.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_PROPERTY_FILTER_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM;

/**
 * Transforms a {@link CatalogRequestMessage} to a {@link JsonObject} in JSON-LD expanded form.
 */
public class JsonObjectFromCatalogRequestMessageTransformer extends AbstractNamespaceAwareJsonLdTransformer<CatalogRequestMessage, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromCatalogRequestMessageTransformer(JsonBuilderFactory jsonFactory) {
        this(jsonFactory, DSPACE_SCHEMA);
    }

    public JsonObjectFromCatalogRequestMessageTransformer(JsonBuilderFactory jsonFactory, String schema) {
        super(CatalogRequestMessage.class, JsonObject.class, schema);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull CatalogRequestMessage message, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder();
        builder.add(TYPE, forNamespace(DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM));

        if (message.getQuerySpec() != null) {
            builder.add(forNamespace(DSPACE_PROPERTY_FILTER_TERM), context.transform(message.getQuerySpec(), JsonObject.class));
        }

        return builder.build();
    }
}
