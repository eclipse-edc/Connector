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
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_OFFER;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CALLBACK_ADDRESS;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_DATASET;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_OFFER;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_OFFER_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.to.TestInput.getExpanded;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToContractRequestMessageTransformerTest {
    private static final String PROCESS_ID = "processId";
    private static final String DATASET_ID = "datasetId";
    private static final String CALLBACK = "https://test.com";
    private static final String OBJECT_ID = "id1";
    private static final String CONTRACT_OFFER_ID = "contractOfferId";

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToContractRequestMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToContractRequestMessageTransformer();
        when(context.problem()).thenReturn(new ProblemBuilder(context));
    }

    @Test
    void verify_usingOffer() {
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, OBJECT_ID)
                .add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE)
                .add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, PROCESS_ID)
                .add(DSPACE_NEGOTIATION_PROPERTY_DATASET, DATASET_ID)
                .add(DSPACE_NEGOTIATION_PROPERTY_CALLBACK_ADDRESS, CALLBACK)
                .add(DSPACE_NEGOTIATION_PROPERTY_OFFER, contractOffer())
                .build();

        when(context.transform(any(JsonObject.class), eq(Policy.class))).thenReturn(policy());

        var result = transformer.transform(getExpanded(message), context);

        assertThat(result).isNotNull();
        assertThat(result.getProtocol()).isNotEmpty();
        assertThat(result.getProcessId()).isEqualTo(PROCESS_ID);
        assertThat(result.getCallbackAddress()).isEqualTo(CALLBACK);
        assertThat(result.getDataSet()).isEqualTo(DATASET_ID);

        var contractOffer = result.getContractOffer();
        assertThat(contractOffer).isNotNull();
        assertThat(contractOffer.getId()).isNotNull();
        assertThat(contractOffer.getPolicy()).isNotNull();
        assertThat(contractOffer.getAssetId()).isEqualTo("target");

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void verify_usingOfferId() {
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, OBJECT_ID)
                .add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE)
                .add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, PROCESS_ID)
                .add(DSPACE_NEGOTIATION_PROPERTY_DATASET, DATASET_ID)
                .add(DSPACE_NEGOTIATION_PROPERTY_CALLBACK_ADDRESS, CALLBACK)
                .add(DSPACE_NEGOTIATION_PROPERTY_OFFER_ID, CONTRACT_OFFER_ID)
                .build();

        var result = transformer.transform(getExpanded(message), context);

        assertThat(result).isNotNull();
        assertThat(result.getProtocol()).isNotEmpty();
        assertThat(result.getProcessId()).isEqualTo(PROCESS_ID);
        assertThat(result.getCallbackAddress()).isEqualTo(CALLBACK);
        assertThat(result.getDataSet()).isEqualTo(DATASET_ID);

        assertThat(result.getContractOfferId()).isNotNull();

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void verify_noCallbackOrDatasetOk() {
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, OBJECT_ID)
                .add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE)
                .add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, PROCESS_ID)
                .add(DSPACE_NEGOTIATION_PROPERTY_OFFER, contractOffer())
                .build();

        when(context.transform(any(JsonObject.class), eq(Policy.class))).thenReturn(policy());

        var result = transformer.transform(getExpanded(message), context);

        assertThat(result).isNotNull();

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void verify_noIdContractOfferFails() {

        var offer = jsonFactory.createObjectBuilder().add(JsonLdKeywords.TYPE, ODRL_POLICY_TYPE_OFFER).build();

        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, OBJECT_ID)
                .add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE)
                .add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, PROCESS_ID)
                .add(DSPACE_NEGOTIATION_PROPERTY_OFFER, offer)
                .build();

        when(context.transform(any(JsonObject.class), eq(Policy.class))).thenReturn(policy());

        var result = transformer.transform(getExpanded(message), context);

        assertThat(result).isNull();

        verify(context, times(1)).reportProblem(anyString());
    }

    @Test
    void transform_nullPolicyFails() {
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, OBJECT_ID)
                .add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_CONTRACT_REQUEST_MESSAGE)
                .add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, PROCESS_ID)
                .add(DSPACE_NEGOTIATION_PROPERTY_DATASET, DATASET_ID)
                .add(DSPACE_NEGOTIATION_PROPERTY_CALLBACK_ADDRESS, CALLBACK)
                .add(DSPACE_NEGOTIATION_PROPERTY_OFFER, contractOffer())
                .build();

        when(context.transform(any(JsonObject.class), eq(Policy.class))).thenReturn(null);

        assertThat(transformer.transform(getExpanded(message), context)).isNull();

        verify(context, times(1)).reportProblem(anyString());
    }

    private JsonObject contractOffer() {
        return jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, CONTRACT_OFFER_ID)
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
