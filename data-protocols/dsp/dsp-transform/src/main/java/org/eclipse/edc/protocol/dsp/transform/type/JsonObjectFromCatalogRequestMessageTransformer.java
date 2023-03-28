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

package org.eclipse.edc.protocol.dsp.transform.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.protocol.dsp.spi.catalog.types.CatalogRequestMessage;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.transformer.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.transform.DspNamespaces.DSPACE_SCHEMA;

public class JsonObjectFromCatalogRequestMessageTransformer extends AbstractJsonLdTransformer<CatalogRequestMessage, JsonObject> {
    
    private static final String DSPACE_CATALOG_REQUEST_TYPE = DSPACE_SCHEMA + "CatalogRequestMessage";
    private static final String DSPACE_FILTER_PROPERTY = DSPACE_SCHEMA + "filter";
    
    private final JsonBuilderFactory jsonFactory;
    private final ObjectMapper mapper;
    
    public JsonObjectFromCatalogRequestMessageTransformer(JsonBuilderFactory jsonFactory, ObjectMapper mapper) {
        super(CatalogRequestMessage.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.mapper = mapper;
    }
    
    @Override
    public @Nullable JsonObject transform(@Nullable CatalogRequestMessage message, @NotNull TransformerContext context) {
        if (message == null) {
            return null;
        }
    
        return jsonFactory.createObjectBuilder()
                .add(TYPE, DSPACE_CATALOG_REQUEST_TYPE)
                .add(DSPACE_FILTER_PROPERTY, mapper.convertValue(message.getFilter(), JsonObject.class))
                .build();
    }
}
