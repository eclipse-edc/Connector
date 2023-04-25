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
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
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
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_AGREEMENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_TIMESTAMP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToContractAgreementMessageTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToContractAgreementMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToContractAgreementMessageTransformer();
    }

    @Test
    void transform() {
        var value = "example";
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, value)
                .add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_AGREEMENT_MESSAGE)
                .add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, value)
                .add(DSPACE_NEGOTIATION_PROPERTY_AGREEMENT, contractAgreement())
                .add(DSPACE_NEGOTIATION_PROPERTY_TIMESTAMP, "123")
                .build();

        when(context.transform(any(JsonObject.class), eq(Policy.class))).thenReturn(policy());

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getClass()).isEqualTo(ContractAgreementMessage.class);
        assertThat(result.getProtocol()).isNotEmpty();
        assertThat(result.getProcessId()).isEqualTo(value);
        assertThat(result.getContractAgreement()).isNotNull();
        assertThat(result.getContractAgreement().getClass()).isEqualTo(ContractAgreement.class);
        assertThat(result.getContractAgreement().getId()).isEqualTo(value);
        assertThat(result.getContractAgreement().getAssetId()).isEqualTo("target");

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void transform_nullPolicy() {
        var value = "example";
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, value)
                .add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_AGREEMENT_MESSAGE)
                .add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, value)
                .add(DSPACE_NEGOTIATION_PROPERTY_AGREEMENT, contractAgreement())
                .add(DSPACE_NEGOTIATION_PROPERTY_TIMESTAMP, "123")
                .build();

        when(context.transform(any(JsonObject.class), eq(Policy.class))).thenReturn(null);

        assertThat(transformer.transform(message, context)).isNull();

        verify(context, times(1)).reportProblem(anyString());
    }

    @Test
    void transform_invalidTimestamp() {
        var value = "example";
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, value)
                .add(JsonLdKeywords.TYPE, DSPACE_NEGOTIATION_AGREEMENT_MESSAGE)
                .add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, value)
                .add(DSPACE_NEGOTIATION_PROPERTY_AGREEMENT, contractAgreement())
                .add(DSPACE_NEGOTIATION_PROPERTY_TIMESTAMP, value)
                .build();

        when(context.transform(any(JsonObject.class), eq(Policy.class))).thenReturn(policy());

        assertThat(transformer.transform(message, context)).isNull();

        verify(context, times(2)).reportProblem(anyString());
    }

    private JsonObject contractAgreement() {
        return jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.TYPE, ODRL_POLICY_TYPE_AGREEMENT)
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