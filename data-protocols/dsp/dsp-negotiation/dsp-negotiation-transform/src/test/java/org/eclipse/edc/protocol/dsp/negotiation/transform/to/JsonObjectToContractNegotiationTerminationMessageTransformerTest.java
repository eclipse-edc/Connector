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
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CODE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_REASON;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_TERMINATION_MESSAGE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JsonObjectToContractNegotiationTerminationMessageTransformerTest {

    public static final String PROCESS_ID = "processId";
    public static final String CODE = "code1";
    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToContractNegotiationTerminationMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToContractNegotiationTerminationMessageTransformer();
    }

    @Test
    void transform() {
        var reason = Json.createBuilderFactory(Map.of()).createObjectBuilder().add("foo", "bar");
        var reasonArray = Json.createBuilderFactory(Map.of()).createArrayBuilder().add(reason).build();

        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, "id1")
                .add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_TERMINATION_MESSAGE)
                .add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, PROCESS_ID)
                .add(DSPACE_NEGOTIATION_PROPERTY_CODE, CODE)
                .add(DSPACE_NEGOTIATION_PROPERTY_REASON, reasonArray)
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();

        assertThat(result.getProtocol()).isNotNull();
        assertThat(result.getCounterPartyAddress()).isNull();
        assertThat(result.getProcessId()).isEqualTo(PROCESS_ID);
        assertThat(result.getCode()).isEqualTo(CODE);

        assertThat(result.getRejectionReason()).isEqualTo("{\"foo\":\"bar\"}");

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void verify_noCodeNodReason() {
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, "id1")
                .add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_TERMINATION_MESSAGE)
                .add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, PROCESS_ID)
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();

        assertThat(result.getProtocol()).isNotNull();
        assertThat(result.getCounterPartyAddress()).isNull();
        assertThat(result.getProcessId()).isEqualTo(PROCESS_ID);
        assertThat(result.getCode()).isNull();
        assertThat(result.getRejectionReason()).isNull();

        verify(context, never()).reportProblem(anyString());
    }
}
