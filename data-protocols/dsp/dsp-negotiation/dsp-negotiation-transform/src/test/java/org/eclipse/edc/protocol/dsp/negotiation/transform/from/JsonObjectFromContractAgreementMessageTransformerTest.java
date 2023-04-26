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
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_AGREEMENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectFromContractAgreementMessageTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectFromContractAgreementMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromContractAgreementMessageTransformer(jsonFactory);
    }

    @Test
    void transform() {
        var value = "example";
        var agreement = contractAgreement();
        var message = ContractAgreementMessage.Builder.newInstance()
                .protocol(value)
                .processId(value)
                .callbackAddress(value)
                .contractAgreement(agreement)
                .build();

        var obj = jsonFactory.createObjectBuilder().build();

        when(context.transform(any(Policy.class), eq(JsonObject.class))).thenReturn(obj);

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(ID).getString()).isNotNull();
        assertThat(result.getJsonString(ID).getString()).isNotEmpty();
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(DSPACE_NEGOTIATION_AGREEMENT_MESSAGE);
        assertThat(result.getJsonString(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID).getString()).isEqualTo(value);
        assertThat(result.getJsonObject(DSPACE_NEGOTIATION_PROPERTY_AGREEMENT)).isNotNull();

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void transform_nullPolicy() {
        var value = "example";
        var agreement = contractAgreement();
        var message = ContractAgreementMessage.Builder.newInstance()
                .protocol(value)
                .processId(value)
                .callbackAddress(value)
                .contractAgreement(agreement)
                .build();

        when(context.transform(any(Policy.class), eq(JsonObject.class))).thenReturn(null);

        assertThat(transformer.transform(message, context)).isNull();

        verify(context, times(1)).reportProblem(anyString());
    }

    private ContractAgreement contractAgreement() {
        return ContractAgreement.Builder.newInstance()
                .id(String.valueOf(UUID.randomUUID()))
                .providerAgentId("agentId")
                .consumerAgentId("agentId")
                .assetId("assetId")
                .policy(policy()).build();
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
                .build();
    }
}