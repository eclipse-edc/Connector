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

package org.eclipse.edc.protocol.dsp.negotiation.transform.to;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.to.TestInput.getExpanded;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON_TERM;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JsonObjectToContractNegotiationTerminationMessageTransformerTest {
    public static final String CODE = "code1";
    private static final JsonLdNamespace DSP_NAMESPACE = new JsonLdNamespace("http://www.w3.org/ns/dsp#");
    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock();

    private final JsonObjectToContractNegotiationTerminationMessageTransformer transformer =
            new JsonObjectToContractNegotiationTerminationMessageTransformer(DSP_NAMESPACE);

    @Test
    void transform() {
        var reason = Json.createBuilderFactory(Map.of()).createObjectBuilder().add(DSP_NAMESPACE.toIri("foo"), "bar");
        var reasonArray = Json.createBuilderFactory(Map.of()).createArrayBuilder().add(reason).build();

        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, "id1")
                .add(JsonLdKeywords.TYPE, DSP_NAMESPACE.toIri(DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_TERM))
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CONSUMER_PID_TERM), "consumerPid")
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_PROVIDER_PID_TERM), "providerPid")
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CODE_TERM), CODE)
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_REASON_TERM), reasonArray)
                .build();

        var result = transformer.transform(getExpanded(message), context);

        assertThat(result).isNotNull();

        assertThat(result.getCounterPartyAddress()).isNull();
        assertThat(result.getConsumerPid()).isEqualTo("consumerPid");
        assertThat(result.getProviderPid()).isEqualTo("providerPid");
        assertThat(result.getCode()).isEqualTo(CODE);

        assertThat(result.getRejectionReason()).isEqualTo(format("[{\"%sfoo\":[{\"@value\":\"bar\"}]}]", DSP_NAMESPACE.namespace()));

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void verify_noCodeNodReason() {
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, "id1")
                .add(JsonLdKeywords.TYPE, DSP_NAMESPACE.toIri(DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_TERM))
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CONSUMER_PID_TERM), "consumerPid")
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_PROVIDER_PID_TERM), "providerPid")
                .build();

        var result = transformer.transform(getExpanded(message), context);

        assertThat(result).isNotNull();

        assertThat(result.getCounterPartyAddress()).isNull();
        assertThat(result.getCode()).isNull();
        assertThat(result.getRejectionReason()).isNull();

        verify(context, never()).reportProblem(anyString());
    }
}
