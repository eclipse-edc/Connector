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

package org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.from;

import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.NegotiationState;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.NegotiationState.NEGOTIATION_STATE_STATE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.NegotiationState.NEGOTIATION_STATE_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.Mockito.mock;

public class JsonObjectFromNegotiationStateTransformerTest {

    private JsonObjectFromNegotiationStateTransformer transformer;


    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromNegotiationStateTransformer(Json.createBuilderFactory(Map.of()));
    }


    @Test
    void transform() {

        var state = "FINALIZED";
        var negotiationState = new NegotiationState(state);
        var context = mock(TransformerContext.class);
        var jsonObject = transformer.transform(negotiationState, context);

        assertThat(jsonObject).isNotNull();
        assertThat(jsonObject.getString(TYPE)).isEqualTo(NEGOTIATION_STATE_TYPE);
        assertThat(jsonObject.getString(NEGOTIATION_STATE_STATE)).isEqualTo(state);
    }
}
