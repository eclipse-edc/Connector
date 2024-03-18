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
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowSuspendMessage;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.api.signaling.transform.TestFunctions.getExpanded;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.CoreConstants.EDC_PREFIX;
import static org.mockito.Mockito.mock;

class JsonObjectToDataFlowSuspendMessageTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);
    private final TitaniumJsonLd jsonLd = new TitaniumJsonLd(mock(Monitor.class));
    private JsonObjectToDataFlowSuspendMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToDataFlowSuspendMessageTransformer();
    }

    @Test
    void transform() {

        var jsonObj = jsonFactory.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, DataFlowSuspendMessage.DATA_FLOW_SUSPEND_MESSAGE_TYPE)
                .add("reason", "reason")
                .build();

        var message = transformer.transform(getExpanded(jsonObj), context);

        assertThat(message).isNotNull();

        assertThat(message.getReason()).isEqualTo("reason");
    }

    @Test
    void transform_withoutReason() {

        var jsonObj = jsonFactory.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, DataFlowSuspendMessage.DATA_FLOW_SUSPEND_MESSAGE_TYPE)
                .build();

        var message = transformer.transform(getExpanded(jsonObj), context);

        assertThat(message).isNotNull();

        assertThat(message.getReason()).isNull();
    }

    private JsonObjectBuilder createContextBuilder() {
        return jsonFactory.createObjectBuilder()
                .add(VOCAB, EDC_NAMESPACE)
                .add(EDC_PREFIX, EDC_NAMESPACE);
    }

}
