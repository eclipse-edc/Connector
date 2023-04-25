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
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CODE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_REASON;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_TERMINATION_MESSAGE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JsonObjectToContractNegotiationTerminationMessageTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToContractNegotiationTerminationMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToContractNegotiationTerminationMessageTransformer();
    }

    @Test
    void transform() {
        var value = "example";
        var reason = Json.createBuilderFactory(Map.of()).createObjectBuilder().add("foo", "bar");

        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, value)
                .add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_TERMINATION_MESSAGE)
                .add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, value)
                .add(DSPACE_NEGOTIATION_PROPERTY_CODE, value)
                .add(DSPACE_NEGOTIATION_PROPERTY_REASON, Json.createBuilderFactory(Map.of()).createArrayBuilder().add(reason).build())
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getClass()).isEqualTo(ContractNegotiationTerminationMessage.class);
        assertThat(result.getProtocol()).isNotNull();
        assertThat(result.getCallbackAddress()).isNull();
        assertThat(result.getProcessId()).isEqualTo(value);
        assertThat(result.getRejectionReason()).isNotNull();
        assertThat(result.getRejectionReason()).isEqualTo("{\"foo\":\"bar\"}");

        verify(context, never()).reportProblem(anyString());
    }
}