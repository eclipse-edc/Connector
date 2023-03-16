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

package org.eclipse.edc.jsonld.transformer.from;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.offer.DataService;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.transformer.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.transformer.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.transformer.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.transformer.Namespaces.DCT_SCHEMA;

public class JsonObjectFromDataServiceTransformer extends AbstractJsonLdTransformer<DataService, JsonObject> {
    
    private final JsonBuilderFactory jsonFactory;
    private final ObjectMapper mapper;
    
    public JsonObjectFromDataServiceTransformer(JsonBuilderFactory jsonFactory, ObjectMapper mapper) {
        super(DataService.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.mapper = mapper;
    }
    
    @Override
    public @Nullable JsonObject transform(@Nullable DataService dataService, @NotNull TransformerContext context) {
        if (dataService == null) {
            return null;
        }
    
        var objectBuilder = jsonFactory.createObjectBuilder();
        objectBuilder.add(ID, dataService.getId());
        objectBuilder.add(TYPE, DCAT_SCHEMA + "DataService");
        
        objectBuilder.add(DCT_SCHEMA + "terms", dataService.getTerms());
        objectBuilder.add(DCT_SCHEMA + "endpointUrl", dataService.getEndpointUrl());
        
        return objectBuilder.build();
    }
    
}
