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

package org.eclipse.edc.protocol.dsp.catalog.transform.to;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.catalog.spi.protocol.CatalogRequestMessage;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_CATALOG_REQUEST_TYPE;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_FILTER_PROPERTY;

/**
 * Transforms a {@link JsonObject} in JSON-LD expanded form to a {@link CatalogRequestMessage}.
 */
public class JsonObjectToCatalogRequestMessageTransformer extends AbstractJsonLdTransformer<JsonObject, CatalogRequestMessage> {
    
    private final ObjectMapper mapper;
    
    public JsonObjectToCatalogRequestMessageTransformer(ObjectMapper mapper) {
        super(JsonObject.class, CatalogRequestMessage.class);
        this.mapper = mapper;
    }
    
    @Override
    public @Nullable CatalogRequestMessage transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var type = nodeType(object, context);
        if (DSPACE_CATALOG_REQUEST_TYPE.equals(type)) {
            return CatalogRequestMessage.Builder.newInstance()
                    .filter(transformQuerySpec(object.get(DSPACE_FILTER_PROPERTY), context))
                    .build();
        } else {
            context.reportProblem(format("Cannot transform type %s to CatalogRequestMessage", type));
            return null;
        }
    }
    
    private QuerySpec transformQuerySpec(JsonValue value, TransformerContext context) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof JsonObject) {
            return mapper.convertValue(value, QuerySpec.class);
        } else if (value instanceof JsonArray) {
            var array = (JsonArray) value;
            return transformQuerySpec(array.getJsonObject(0), context);
        } else {
            context.reportProblem(format("Expected filter to be JsonObject or JsonArray, but was %s", value.getClass().getSimpleName()));
            return null;
        }
    }
}
