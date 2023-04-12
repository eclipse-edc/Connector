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
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.jsonld.JsonLdKeywords;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CALLBACK_ADDRESS;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_DATASET;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_OFFER;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.transform.transformer.PropertyAndTypeNames.ODRL_POLICY_TYPE_OFFER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToContractRequestMessageTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToContractRequestMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToContractRequestMessageTransformer();
    }

    @Test
    void transform() {
        var value = "example";
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, value)
                .add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE)
                .add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, value)
                .add(DSPACE_NEGOTIATION_PROPERTY_DATASET, value)
                .add(DSPACE_NEGOTIATION_PROPERTY_CALLBACK_ADDRESS, value)
                .add(DSPACE_NEGOTIATION_PROPERTY_OFFER, contractOffer())
                .build();

        when(context.transform(any(JsonObject.class), eq(Policy.class))).thenReturn(policy());

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getClass()).isEqualTo(ContractRequestMessage.class);
        assertThat(result.getProtocol()).isNotEmpty();
        assertThat(result.getProcessId()).isEqualTo(value);
        assertThat(result.getCallbackAddress()).isEqualTo(value);
        assertThat(result.getDataSet()).isEqualTo(value);
        assertThat(result.getContractOffer()).isNotNull();
        assertThat(result.getContractOffer().getClass()).isEqualTo(ContractOffer.class);
        assertThat(result.getContractOffer().getPolicy()).isNotNull();
        assertThat(result.getContractOffer().getAsset().getId()).isEqualTo("target");

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void transform_nullPolicy() {
        var value = "example";
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, value)
                .add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE)
                .add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, value)
                .add(DSPACE_NEGOTIATION_PROPERTY_DATASET, value)
                .add(DSPACE_NEGOTIATION_PROPERTY_CALLBACK_ADDRESS, value)
                .add(DSPACE_NEGOTIATION_PROPERTY_OFFER, contractOffer())
                .build();

        when(context.transform(any(JsonObject.class), eq(Policy.class))).thenReturn(null);

        assertThat(transformer.transform(message, context)).isNull();

        verify(context, times(1)).reportProblem(anyString());
    }

    private JsonObject contractOffer() {
        return jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.TYPE, ODRL_POLICY_TYPE_OFFER)
                .build();
    }

    private Policy policy() {
        var action = Action.Builder.newInstance().type("USE").build();
        var permission = Permission.Builder.newInstance().action(action).build();
        var prohibition = Prohibition.Builder.newInstance().action(action).build();
        var duty = Duty.Builder.newInstance().action(action).build();
        return Policy.Builder.newInstance()
                .permission(permission)
                .prohibition(prohibition)
                .duty(duty)
                .target("target")
                .build();
    }
}