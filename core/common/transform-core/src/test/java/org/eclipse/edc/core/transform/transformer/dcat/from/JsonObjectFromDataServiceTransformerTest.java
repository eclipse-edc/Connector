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

package org.eclipse.edc.core.transform.transformer.dcat.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.catalog.spi.DataService;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DATA_SERVICE_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_ENDPOINT_URL_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_TERMS_ATTRIBUTE;
import static org.mockito.Mockito.mock;

class JsonObjectFromDataServiceTransformerTest {
    
    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private TransformerContext context = mock(TransformerContext.class);
    
    private JsonObjectFromDataServiceTransformer transformer;
    
    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromDataServiceTransformer(jsonFactory);
    }
    
    @Test
    void transform_returnJsonObject() {
        var dataService = DataService.Builder.newInstance()
                .id("dataServiceId")
                .terms("terms")
                .endpointUrl("url")
                .build();
        var result = transformer.transform(dataService, context);
        
        assertThat(result).isNotNull();
        assertThat(result.getJsonString(ID).getString()).isEqualTo(dataService.getId());
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(DCAT_DATA_SERVICE_TYPE);
        assertThat(result.getJsonString(DCT_TERMS_ATTRIBUTE).getString()).isEqualTo(dataService.getTerms());
        assertThat(result.getJsonString(DCT_ENDPOINT_URL_ATTRIBUTE).getString()).isEqualTo(dataService.getEndpointUrl());
    }
}
