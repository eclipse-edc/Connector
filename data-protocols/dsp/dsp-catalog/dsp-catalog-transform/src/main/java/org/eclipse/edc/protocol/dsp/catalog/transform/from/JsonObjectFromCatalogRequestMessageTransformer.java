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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.protocol.dsp.catalog.spi.types.CatalogRequestMessage;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_CATALOG_REQUEST_TYPE;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_FILTER_PROPERTY;

/**
 * Transforms a {@link CatalogRequestMessage} to a {@link JsonObject} in JSON-LD expanded form.
 */
public class JsonObjectFromCatalogRequestMessageTransformer extends AbstractJsonLdTransformer<CatalogRequestMessage, JsonObject> {
    
    private final JsonBuilderFactory jsonFactory;
    private final ObjectMapper mapper;
    
    public JsonObjectFromCatalogRequestMessageTransformer(JsonBuilderFactory jsonFactory, ObjectMapper mapper) {
        super(CatalogRequestMessage.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.mapper = mapper;
    }
    
    @Override
    public @Nullable JsonObject transform(@NotNull CatalogRequestMessage message, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder();
        builder.add(TYPE, DSPACE_CATALOG_REQUEST_TYPE);
        
        if (message.getFilter() != null) {
            builder.add(DSPACE_FILTER_PROPERTY, mapper.convertValue(message.getFilter(), JsonObject.class));
        }
        
        return builder.build();
    }
}
