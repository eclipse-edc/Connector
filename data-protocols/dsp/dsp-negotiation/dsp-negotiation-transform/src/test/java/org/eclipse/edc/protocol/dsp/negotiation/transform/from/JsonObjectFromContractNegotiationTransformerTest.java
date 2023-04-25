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

package org.eclipse.edc.protocol.dsp.negotiation.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_CONTRACT_NEGOTIATION;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CHECKSUM;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_STATE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_STATE_REQUESTED;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JsonObjectFromContractNegotiationTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectFromContractNegotiationTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromContractNegotiationTransformer(jsonFactory);
    }

    @Test
    void transform() {
        var value = "example";
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id(value)
                .correlationId(value)
                .counterPartyId("counterPartyId")
                .counterPartyAddress("counterPartyAddress")
                .protocol("protocol")
                .state(REQUESTED.code())
                .checksum(value)
                .build();

        var result = transformer.transform(negotiation, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(ID).getString()).isEqualTo(value);
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(DSPACE_CONTRACT_NEGOTIATION);
        assertThat(result.getJsonString(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID).getString()).isEqualTo(value);
        assertThat(result.getJsonString(DSPACE_NEGOTIATION_PROPERTY_STATE).getString()).isEqualTo(DSPACE_NEGOTIATION_STATE_REQUESTED);
        assertThat(result.getJsonString(DSPACE_NEGOTIATION_PROPERTY_CHECKSUM).getString()).isEqualTo(value);

        verify(context, never()).reportProblem(anyString());
    }
}