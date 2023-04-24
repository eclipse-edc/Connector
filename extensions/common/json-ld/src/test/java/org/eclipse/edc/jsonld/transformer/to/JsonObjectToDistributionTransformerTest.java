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

package org.eclipse.edc.jsonld.transformer.to;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_ACCESS_SERVICE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_DISTRIBUTION_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class JsonObjectToDistributionTransformerTest {
    
    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private TransformerContext context = mock(TransformerContext.class);
    
    private JsonObjectToDistributionTransformer transformer;
    
    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToDistributionTransformer();
    }
    
    @Test
    void transform_returnDistribution() {
        var format = "format";
        var dataServiceId = "dataServiceId";
        
        var distribution = jsonFactory.createObjectBuilder()
                .add(TYPE, DCAT_DISTRIBUTION_TYPE)
                .add(DCT_FORMAT_ATTRIBUTE, format)
                .add(DCAT_ACCESS_SERVICE_ATTRIBUTE, dataServiceId)
                .build();
    
        var result = transformer.transform(distribution, context);
    
        assertThat(result).isNotNull();
        assertThat(result.getFormat()).isEqualTo(format);
        assertThat(result.getDataService()).isNotNull()
                .matches(ds -> ds.getId().equals(dataServiceId) && ds.getTerms() == null && ds.getEndpointUrl() == null);
        
        verifyNoInteractions(context);
    }
    
    @Test
    void transform_invalidType_reportProblem() {
        var distribution = jsonFactory.createObjectBuilder()
                .add(TYPE, "not-a-distribution")
                .build();
    
        transformer.transform(distribution, context);
        
        verify(context, times(1)).reportProblem(anyString());
    }
    
    @Test
    void transform_requiredAttributesMissing_reportProblem() {
        var distribution = jsonFactory.createObjectBuilder()
                .add(TYPE, DCAT_DISTRIBUTION_TYPE)
                .build();
        
        var result = transformer.transform(distribution, context);
    
        assertThat(result).isNull();
        verify(context, times(1)).reportProblem(anyString());
    }
}
