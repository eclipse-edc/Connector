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
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JsonObjectFromContractNegotiationTerminationMessageTransformerTest {
    private static final String REJECTION_REASON = "rejection";
    private static final String REJECTION_CODE = "1";
    private static final String DSP = "DSP";

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock();

    private final JsonObjectFromContractNegotiationTerminationMessageTransformer transformer =
            new JsonObjectFromContractNegotiationTerminationMessageTransformer(jsonFactory);

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
        assertThat(result.getString(TYPE)).isEqualTo(DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE);
        assertThat(result.getString(DSPACE_PROPERTY_CONSUMER_PID)).isEqualTo("consumerPid");
        assertThat(result.getString(DSPACE_PROPERTY_PROVIDER_PID)).isEqualTo("providerPid");
        assertThat(result.getString(DSPACE_PROPERTY_CODE)).isEqualTo(REJECTION_CODE);
        assertThat(result.getString(DSPACE_PROPERTY_REASON)).isEqualTo(REJECTION_REASON);

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
        assertThat(result.getJsonString(DSPACE_PROPERTY_CODE)).isNull();
        assertThat(result.getJsonString(DSPACE_PROPERTY_REASON)).isNull();

        verify(context, never()).reportProblem(anyString());
    }
}
