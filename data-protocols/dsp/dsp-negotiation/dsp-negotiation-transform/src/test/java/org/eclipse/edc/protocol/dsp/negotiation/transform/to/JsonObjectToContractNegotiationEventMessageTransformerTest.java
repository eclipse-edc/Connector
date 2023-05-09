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
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_EVENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_ACCEPTED;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class JsonObjectToContractNegotiationEventMessageTransformerTest {
    private static final String PROCESS_ID = "processId";

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToContractNegotiationEventMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToContractNegotiationEventMessageTransformer();
    }

    @Test
    void transform() {
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, "id1")
                .add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_EVENT_MESSAGE)
                .add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, PROCESS_ID)
                .add(DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE, DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_ACCEPTED)
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getClass()).isEqualTo(ContractNegotiationEventMessage.class);
        assertThat(result.getProtocol()).isNotEmpty();
        assertThat(result.getProcessId()).isEqualTo(PROCESS_ID);
        assertThat(result.getType()).isEqualTo(ContractNegotiationEventMessage.Type.ACCEPTED);

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void verify_failTransformWhenProcessIdMissing() {
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, "id1")
                .add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_EVENT_MESSAGE)
                .add(DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE, DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE_ACCEPTED)
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNull();

        verify(context, times(1)).reportProblem(contains(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID));
    }

    @Test
    void verify_failTransformWhenEventTypeMissing() {
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, "id1")
                .add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_EVENT_MESSAGE)
                .add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, PROCESS_ID)
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNull();

        verify(context, times(1)).reportProblem(contains(DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE));
    }

    @Test
    void verify_failTransformWhenEventTypeInvalid() {
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, "id1")
                .add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_EVENT_MESSAGE)
                .add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, PROCESS_ID)
                .add(DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE, "InvalidType")
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNull();

        verify(context, times(1)).reportProblem(contains(DSPACE_NEGOTIATION_PROPERTY_EVENT_TYPE));
    }
}
