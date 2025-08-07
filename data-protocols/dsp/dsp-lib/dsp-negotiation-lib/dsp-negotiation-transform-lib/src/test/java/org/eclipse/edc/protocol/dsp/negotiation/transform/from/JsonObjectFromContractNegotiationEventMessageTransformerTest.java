/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.negotiation.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractNegotiationEventMessage.Type.ACCEPTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_EVENT_TYPE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_ACCEPTED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JsonObjectFromContractNegotiationEventMessageTransformerTest {
    private static final String DSP = "DSP";
    private static final JsonLdNamespace DSP_NAMESPACE = new JsonLdNamespace("http://www.w3.org/ns/dsp#");

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock();

    private final JsonObjectFromContractNegotiationEventMessageTransformer transformer =
            new JsonObjectFromContractNegotiationEventMessageTransformer(jsonFactory, DSP_NAMESPACE);

    @Test
    void transform() {
        var message = ContractNegotiationEventMessage.Builder.newInstance()
                .protocol(DSP)
                .consumerPid("consumerPid")
                .providerPid("providerPid")
                .counterPartyAddress("https://test.com")
                .policy(Policy.Builder.newInstance().build())
                .type(ACCEPTED)
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isNotEmpty();
        assertThat(result.getString(TYPE)).isEqualTo(DSP_NAMESPACE.toIri(DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_TERM));
        assertThat(result.getString(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_PROVIDER_PID_TERM))).isEqualTo("providerPid");
        assertThat(result.getString(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CONSUMER_PID_TERM))).isEqualTo("consumerPid");
        assertThat(result.getString(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_EVENT_TYPE_TERM)))
                .isEqualTo(DSP_NAMESPACE.toIri(DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_ACCEPTED_TERM));

        verify(context, never()).reportProblem(anyString());
    }
}
