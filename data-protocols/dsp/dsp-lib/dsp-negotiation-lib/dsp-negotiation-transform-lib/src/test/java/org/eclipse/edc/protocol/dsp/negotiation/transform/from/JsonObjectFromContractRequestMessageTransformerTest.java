/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.negotiation.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.from.TestFunction.DSP_NAMESPACE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.from.TestFunction.toIri;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_OFFER_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectFromContractRequestMessageTransformerTest {

    private static final String CALLBACK_ADDRESS = "https://test.com";
    private static final String CONSUMER_PID = "consumerPid";
    private static final String PROTOCOL = "DSP";
    private static final String DATASET_ID = "datasetId";
    private static final String CONTRACT_OFFER_ID = "contractOffer1";

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock();

    private final JsonObjectFromContractRequestMessageTransformer transformer =
            new JsonObjectFromContractRequestMessageTransformer(jsonFactory, DSP_NAMESPACE);

    @BeforeEach
    void setUp() {
        when(context.problem()).thenReturn(new ProblemBuilder(context));
    }

    @Test
    void verify_contractOffer() {
        var message = contractRequestMessageBuilder()
                .consumerPid("consumerPid")
                .providerPid("providerPid")
                .contractOffer(contractOffer())
                .build();
        var obj = jsonFactory.createObjectBuilder().build();
        when(context.transform(any(Policy.class), eq(JsonObject.class))).thenReturn(obj);

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(ID).getString()).isNotEmpty();
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(toIri(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_TERM));
        assertThat(result.getJsonString(toIri(DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM)).getString()).isEqualTo(CALLBACK_ADDRESS);
        assertThat(result.getJsonObject(toIri(DSPACE_PROPERTY_OFFER_TERM))).isNotNull();
        assertThat(result.getJsonObject(toIri(DSPACE_PROPERTY_OFFER_TERM)).getString(ID)).isEqualTo(CONTRACT_OFFER_ID);
        assertThat(result.getJsonObject(toIri(DSPACE_PROPERTY_CONSUMER_PID_TERM)).getString(ID)).isEqualTo("consumerPid");
        assertThat(result.getJsonObject(toIri(DSPACE_PROPERTY_PROVIDER_PID_TERM)).getString(ID)).isEqualTo("providerPid");

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void shouldFail_whenPolicyCannotBeTransformed() {
        var message = contractRequestMessageBuilder()
                .processId("processId")
                .consumerPid(CONSUMER_PID)
                .contractOffer(contractOffer())
                .build();
        when(context.transform(any(Policy.class), eq(JsonObject.class))).thenReturn(null);

        var result = transformer.transform(message, context);

        assertThat(result).isNull();
        verify(context, times(1)).reportProblem(anyString());
    }

    private ContractRequestMessage.Builder contractRequestMessageBuilder() {
        return ContractRequestMessage.Builder.newInstance()
                .protocol(PROTOCOL)
                .callbackAddress(CALLBACK_ADDRESS);
    }

    private ContractOffer contractOffer() {
        return ContractOffer.Builder.newInstance()
                .id(CONTRACT_OFFER_ID)
                .assetId(DATASET_ID)
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

}
