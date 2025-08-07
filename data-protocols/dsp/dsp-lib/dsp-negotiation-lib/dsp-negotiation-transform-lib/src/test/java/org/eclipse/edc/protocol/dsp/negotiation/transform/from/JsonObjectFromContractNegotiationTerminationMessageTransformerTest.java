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
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON_TERM;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JsonObjectFromContractNegotiationTerminationMessageTransformerTest {
    private static final String REJECTION_REASON = "rejection";
    private static final String REJECTION_CODE = "1";
    private static final String DSP = "DSP";
    private static final JsonLdNamespace DSP_NAMESPACE = new JsonLdNamespace("http://www.w3.org/ns/dsp#");

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock();

    private final JsonObjectFromContractNegotiationTerminationMessageTransformer transformer =
            new JsonObjectFromContractNegotiationTerminationMessageTransformer(jsonFactory, DSP_NAMESPACE);

    @Test
    void transform() {
        var message = ContractNegotiationTerminationMessage.Builder.newInstance()
                .protocol(DSP)
                .consumerPid("consumerPid")
                .providerPid("providerPid")
                .counterPartyAddress("https://test.com")
                .rejectionReason(REJECTION_REASON)
                .code(REJECTION_CODE)
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isNotEmpty();
        assertThat(result.getString(TYPE)).isEqualTo(DSP_NAMESPACE.toIri(DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_TERM));
        assertThat(result.getString(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CONSUMER_PID_TERM))).isEqualTo("consumerPid");
        assertThat(result.getString(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_PROVIDER_PID_TERM))).isEqualTo("providerPid");
        assertThat(result.getString(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CODE_TERM))).isEqualTo(REJECTION_CODE);
        assertThat(result.getString(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_REASON_TERM))).isEqualTo(REJECTION_REASON);

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void verify_noReasonNoCode() {
        var message = ContractNegotiationTerminationMessage.Builder.newInstance()
                .protocol(DSP)
                .processId("processId")
                .consumerPid("consumerPid")
                .providerPid("providerPid")
                .counterPartyAddress("https://test.com")
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CODE_TERM))).isNull();
        assertThat(result.getJsonString(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_REASON_TERM))).isNull();

        verify(context, never()).reportProblem(anyString());
    }
}
