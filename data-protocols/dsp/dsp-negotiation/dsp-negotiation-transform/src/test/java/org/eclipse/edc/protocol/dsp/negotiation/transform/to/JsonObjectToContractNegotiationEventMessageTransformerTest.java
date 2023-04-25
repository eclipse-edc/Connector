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

package org.eclipse.edc.protocol.dsp.negotiation.transform.to;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_EVENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CHECKSUM;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_ACCEPTED;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class JsonObjectToContractNegotiationEventMessageTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToContractNegotiationEventMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToContractNegotiationEventMessageTransformer();
    }

    @Test
    void transform() {
        var value = "example";
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, value)
                .add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_EVENT_MESSAGE)
                .add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, value)
                .add(DSPACE_NEGOTIATION_PROPERTY_CHECKSUM, value)
                .add(DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE, DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_ACCEPTED)
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getClass()).isEqualTo(ContractNegotiationEventMessage.class);
        assertThat(result.getProtocol()).isNotEmpty();
        assertThat(result.getProcessId()).isEqualTo(value);
        assertThat(result.getChecksum()).isEqualTo(value);
        assertThat(result.getType()).isEqualTo(ContractNegotiationEventMessage.Type.ACCEPTED);

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void transform_invalidType() {
        var value = "example";
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, value)
                .add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_EVENT_MESSAGE)
                .add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, value)
                .add(DSPACE_NEGOTIATION_PROPERTY_CHECKSUM, value)
                .add(DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE, "INVALID_TYPE")
                .build();

        assertThat(transformer.transform(message, context)).isNull();

        verify(context, times(1)).reportProblem(anyString());
    }
}