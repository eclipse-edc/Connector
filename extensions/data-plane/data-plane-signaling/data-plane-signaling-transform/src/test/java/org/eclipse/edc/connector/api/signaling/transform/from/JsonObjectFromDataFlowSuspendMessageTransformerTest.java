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
import org.eclipse.edc.spi.types.domain.transfer.DataFlowSuspendMessage;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowSuspendMessage.DATA_FLOW_SUSPEND_MESSAGE_REASON;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowSuspendMessage.DATA_FLOW_SUSPEND_MESSAGE_TYPE;
import static org.mockito.Mockito.mock;

class JsonObjectFromDataFlowSuspendMessageTransformerTest {


    private final TransformerContext context = mock(TransformerContext.class);
    private JsonObjectFromDataFlowSuspendMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromDataFlowSuspendMessageTransformer(Json.createBuilderFactory(Map.of()));
    }

    @Test
    void transform() {

        var message = DataFlowSuspendMessage.Builder.newInstance().reason("reason").build();

        var jsonObject = transformer.transform(message, context);

        assertThat(jsonObject).isNotNull();

        assertThat(jsonObject.getJsonString(TYPE).getString()).isEqualTo(DATA_FLOW_SUSPEND_MESSAGE_TYPE);
        assertThat(jsonObject.getJsonString(DATA_FLOW_SUSPEND_MESSAGE_REASON).getString()).isEqualTo("reason");

    }

    @Test
    void transform_withoutReason() {

        var message = DataFlowSuspendMessage.Builder.newInstance().build();

        var jsonObject = transformer.transform(message, context);

        assertThat(jsonObject).isNotNull();

        assertThat(jsonObject.getJsonString(TYPE).getString()).isEqualTo(DATA_FLOW_SUSPEND_MESSAGE_TYPE);
        assertThat(jsonObject.containsKey(DATA_FLOW_SUSPEND_MESSAGE_REASON)).isFalse();

    }

}
