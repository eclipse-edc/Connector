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

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_PROPERTY_FILTER_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectFromCatalogRequestMessageTransformerTest {

    private static final JsonLdNamespace DSP_NAMESPACE = new JsonLdNamespace("http://www.w3.org/ns/dsp#");
    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);
    private JsonObjectFromCatalogRequestMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromCatalogRequestMessageTransformer(jsonFactory, DSP_NAMESPACE);
    }

    @Test
    void transform_returnJsonObject() {
        var querySpec = QuerySpec.Builder.newInstance().build();
        var querySpecJson = jsonFactory.createObjectBuilder().build();
        when(context.transform(querySpec, JsonObject.class)).thenReturn(querySpecJson);

        var message = CatalogRequestMessage.Builder.newInstance()
                .protocol("protocol")
                .querySpec(querySpec)
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(DSP_NAMESPACE.toIri(DSPACE_TYPE_CATALOG_REQUEST_MESSAGE_TERM));
        assertThat(result.get(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_FILTER_TERM))).isEqualTo(querySpecJson);
        verify(context, never()).reportProblem(anyString());
    }
}
