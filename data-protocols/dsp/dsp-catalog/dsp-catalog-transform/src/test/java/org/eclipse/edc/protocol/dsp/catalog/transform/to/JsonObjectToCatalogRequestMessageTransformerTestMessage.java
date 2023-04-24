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
import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_CATALOG_REQUEST_TYPE;
import static org.eclipse.edc.protocol.dsp.catalog.transform.DspCatalogPropertyAndTypeNames.DSPACE_FILTER_PROPERTY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToCatalogRequestMessageTransformerTestMessage {

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private ObjectMapper mapper = mock(ObjectMapper.class);
    private TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToCatalogRequestMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToCatalogRequestMessageTransformer(mapper);
    }

    @Test
    void transform_noFilter_returnCatalogRequestMessage() {
        var message = jsonFactory.createObjectBuilder()
                .add(TYPE, DSPACE_CATALOG_REQUEST_TYPE)
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getFilter()).isNull();

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void transform_withFilter_returnCatalogRequestMessage() {
        var querySpecJson = jsonFactory.createObjectBuilder().build();
        var querySpec = QuerySpec.Builder.newInstance().build();
        when(mapper.convertValue(querySpecJson, QuerySpec.class)).thenReturn(querySpec);

        var message = jsonFactory.createObjectBuilder()
                .add(TYPE, DSPACE_CATALOG_REQUEST_TYPE)
                .add(DSPACE_FILTER_PROPERTY, querySpecJson)
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getFilter()).isNotNull().isEqualTo(querySpec);

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void transform_invalidType_reportProblem() {
        var message = jsonFactory.createObjectBuilder()
                .add(TYPE, "not-a-catalog-request-message")
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNull();
        verify(context, times(1)).reportProblem(anyString());
    }
}
