/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.signaling.transform.to;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.api.signaling.transform.TestFunctions.getExpanded;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage.DATA_FLOW_RESPONSE_MESSAGE_SIMPLE_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToDataFlowResponseMessageTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);
    private JsonObjectToDataFlowResponseMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToDataFlowResponseMessageTransformer();
        when(context.transform(any(JsonObject.class), eq(DataAddress.class))).thenReturn(DataAddress.Builder.newInstance().type("type").build());
    }

    @Test
    void transform() {
        var jsonObj = jsonFactory.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, DATA_FLOW_RESPONSE_MESSAGE_SIMPLE_TYPE)
                .add("provisioning", true)
                .add("dataAddress", jsonFactory.createObjectBuilder().build())
                .build();

        var message = transformer.transform(getExpanded(jsonObj), context);

        assertThat(message).isNotNull();
        assertThat(message.getDataAddress()).isNotNull();
        assertThat(message.isProvisioning()).isTrue();
    }

    @Test
    void transform_withoutDataAddress() {
        var jsonObj = jsonFactory.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, DATA_FLOW_RESPONSE_MESSAGE_SIMPLE_TYPE)
                .build();

        var message = transformer.transform(getExpanded(jsonObj), context);

        assertThat(message).isNotNull();
        assertThat(message.getDataAddress()).isNull();
    }

    private JsonObjectBuilder createContextBuilder() {
        return jsonFactory.createObjectBuilder()
                .add(VOCAB, EDC_NAMESPACE)
                .add(EDC_PREFIX, EDC_NAMESPACE);
    }

}
