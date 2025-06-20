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

package org.eclipse.edc.protocol.dsp.negotiation.transform.to;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.to.TestInput.getExpanded;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_IRI;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_STATE_IRI;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class JsonObjectToContractNegotiationAckTransformerTest {

    private final TransformerContext context = mock();
    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final JsonObjectToContractNegotiationAckTransformer transformer = new JsonObjectToContractNegotiationAckTransformer();

    @Test
    void shouldTransform() {
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.TYPE, DSPACE_TYPE_CONTRACT_NEGOTIATION_IRI)
                .add(DSPACE_PROPERTY_CONSUMER_PID_IRI, "consumerPid")
                .add(DSPACE_PROPERTY_PROVIDER_PID_IRI, "providerPid")
                .add(DSPACE_PROPERTY_STATE_IRI, "STATE")
                .build();

        var result = transformer.transform(getExpanded(message), context);

        assertThat(result).isNotNull();
        assertThat(result.getConsumerPid()).isEqualTo("consumerPid");
        assertThat(result.getProviderPid()).isEqualTo("providerPid");
        assertThat(result.getState()).isEqualTo("STATE");
        verifyNoInteractions(context);
    }

}
