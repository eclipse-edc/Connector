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
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.protocol.dsp.spi.catalog.types.CatalogRequestMessage;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.transformer.JsonLdFunctions.nodeType;
import static org.eclipse.edc.protocol.dsp.transform.DspNamespaces.DSPACE_SCHEMA;

public class JsonObjectToCatalogRequestMessageTransformer extends AbstractJsonLdTransformer<JsonObject, CatalogRequestMessage> {
    
    private static final String DSPACE_CATALOG_REQUEST_TYPE = DSPACE_SCHEMA + "CatalogRequestMessage";
    private static final String DSPACE_FILTER_PROPERTY = DSPACE_SCHEMA + "filter";
    
    private ObjectMapper mapper;
    
    public JsonObjectToCatalogRequestMessageTransformer(ObjectMapper mapper) {
        super(JsonObject.class, CatalogRequestMessage.class);
        this.mapper = mapper;
    }
    
    @Override
    public @Nullable CatalogRequestMessage transform(@Nullable JsonObject object, @NotNull TransformerContext context) {
        if (object == null) {
            return null;
        }
        
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
