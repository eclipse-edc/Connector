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

package org.eclipse.edc.connector.api.signaling.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage.DATA_FLOW_RESPONSE_MESSAGE_DATA_ADDRESS;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage.DATA_FLOW_RESPONSE_MESSAGE_PROVISIONING;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage.DATA_FLOW_RESPONSE_MESSAGE_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectFromDataFlowResponseMessageTransformerTest {

    private final TransformerContext context = mock(TransformerContext.class);
    private JsonObjectFromDataFlowResponseMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromDataFlowResponseMessageTransformer(Json.createBuilderFactory(Map.of()));
        when(context.transform(any(DataAddress.class), eq(JsonObject.class))).thenReturn(Json.createObjectBuilder().build());
    }

    @Test
    void transform() {
        var message = DataFlowResponseMessage.Builder.newInstance()
                .dataAddress(DataAddress.Builder.newInstance().type("type").build())
                .provisioning(true)
                .build();

        var jsonObject = transformer.transform(message, context);

        assertThat(jsonObject).isNotNull();
        assertThat(jsonObject.getJsonString(TYPE).getString()).isEqualTo(DATA_FLOW_RESPONSE_MESSAGE_TYPE);
        assertThat(jsonObject.get(DATA_FLOW_RESPONSE_MESSAGE_DATA_ADDRESS)).isNotNull();
        assertThat(jsonObject.getBoolean(DATA_FLOW_RESPONSE_MESSAGE_PROVISIONING)).isTrue();
    }

    @Test
    void transform_withoutDataAddress() {
        var message = DataFlowResponseMessage.Builder.newInstance().build();

        var jsonObject = transformer.transform(message, context);

        assertThat(jsonObject).isNotNull();

        assertThat(jsonObject.getJsonString(TYPE).getString()).isEqualTo(DATA_FLOW_RESPONSE_MESSAGE_TYPE);
        assertThat(jsonObject.containsKey(DATA_FLOW_RESPONSE_MESSAGE_DATA_ADDRESS)).isFalse();
    }

}
