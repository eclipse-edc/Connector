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
import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.catalog.spi.protocol.CatalogRequestMessage;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_CATALOG_REQUEST_TYPE;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_FILTER_PROPERTY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectFromCatalogRequestMessageTransformerTest {
    
    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final ObjectMapper mapper = mock(ObjectMapper.class);
    private final TransformerContext context = mock(TransformerContext.class);
    
    private JsonObjectFromCatalogRequestMessageTransformer transformer;
    
    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromCatalogRequestMessageTransformer(jsonFactory, mapper);
    }
    
    @Test
    void transform_noFilter_returnJsonObject() {
        var message = CatalogRequestMessage.Builder.newInstance().build();
        
        var result = transformer.transform(message, context);
        
        assertThat(result).isNotNull();
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(DSPACE_CATALOG_REQUEST_TYPE);
        assertThat(result.get(DSPACE_FILTER_PROPERTY)).isNull();
        
        verify(context, never()).reportProblem(anyString());
    }
    
    @Test
    void transform_withFilter_returnJsonObject() {
        var querySpec = QuerySpec.Builder.newInstance().build();
        var querySpecJson = jsonFactory.createObjectBuilder().build();
        when(mapper.convertValue(querySpec, JsonObject.class)).thenReturn(querySpecJson);
    
        var message = CatalogRequestMessage.Builder.newInstance()
                .filter(querySpec)
                .build();
    
        var result = transformer.transform(message, context);
    
        assertThat(result).isNotNull();
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(DSPACE_CATALOG_REQUEST_TYPE);
        assertThat(result.get(DSPACE_FILTER_PROPERTY)).isEqualTo(querySpecJson);
    
        verify(context, never()).reportProblem(anyString());
    }
}
