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
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.spi.types.domain.agreement.ContractAgreement;
import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.transform.to.TestInput.getExpanded;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_AGREEMENT;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_ID;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_ID;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_PROPERTY_TIMESTAMP;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToContractAgreementMessageTransformerTest {
    private static final String CONSUMER_ID = "consumerId";
    private static final String PROVIDER_ID = "providerId";
    private static final String AGREEMENT_ID = "agreementId";
    private static final String MESSAGE_ID = "messageId";
    private static final String TIMESTAMP = "1970-01-01T00:00:00Z";
    private static final String TARGET = "target";

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToContractAgreementMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToContractAgreementMessageTransformer();
        when(context.problem()).thenReturn(new ProblemBuilder(context));
    }

    @Test
    void transform() {
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, MESSAGE_ID)
                .add(JsonLdKeywords.TYPE, DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE)
                .add(DSPACE_PROPERTY_CONSUMER_PID, "consumerPid")
                .add(DSPACE_PROPERTY_PROVIDER_PID, "providerPid")
                .add(DSPACE_PROPERTY_AGREEMENT, contractAgreement())
                .build();

        when(context.transform(any(JsonObject.class), eq(Policy.class))).thenReturn(policy());

        var result = transformer.transform(getExpanded(message), context);

        assertThat(result).isNotNull();
        assertThat(result.getClass()).isEqualTo(ContractAgreementMessage.class);
        assertThat(result.getProtocol()).isNotEmpty();
        assertThat(result.getConsumerPid()).isEqualTo("consumerPid");
        assertThat(result.getProviderPid()).isEqualTo("providerPid");

        var agreement = result.getContractAgreement();
        assertThat(agreement).isNotNull();
        assertThat(agreement.getClass()).isEqualTo(ContractAgreement.class);
        assertThat(agreement.getId()).isEqualTo(AGREEMENT_ID);
        assertThat(agreement.getConsumerId()).isEqualTo(CONSUMER_ID);
        assertThat(agreement.getProviderId()).isEqualTo(PROVIDER_ID);
        assertThat(agreement.getAssetId()).isEqualTo(TARGET);
        assertThat(agreement.getContractSigningDate()).isEqualTo(Instant.parse(TIMESTAMP).getEpochSecond());

        verify(context, never()).reportProblem(anyString());
    }

    @Deprecated(since = "0.4.1")
    @Test
    void transform_processId() {
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, MESSAGE_ID)
                .add(JsonLdKeywords.TYPE, DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE)
                .add(DSPACE_PROPERTY_PROCESS_ID, "processId")
                .add(DSPACE_PROPERTY_AGREEMENT, contractAgreement())
                .build();

        when(context.transform(any(JsonObject.class), eq(Policy.class))).thenReturn(policy());

        var result = transformer.transform(getExpanded(message), context);

        assertThat(result).isNotNull();
        assertThat(result.getClass()).isEqualTo(ContractAgreementMessage.class);
        assertThat(result.getProtocol()).isNotEmpty();
        assertThat(result.getConsumerPid()).isEqualTo("processId");
        assertThat(result.getProviderPid()).isEqualTo("processId");

        var agreement = result.getContractAgreement();
        assertThat(agreement).isNotNull();
        assertThat(agreement.getClass()).isEqualTo(ContractAgreement.class);
        assertThat(agreement.getId()).isEqualTo(AGREEMENT_ID);
        assertThat(agreement.getConsumerId()).isEqualTo(CONSUMER_ID);
        assertThat(agreement.getProviderId()).isEqualTo(PROVIDER_ID);
        assertThat(agreement.getAssetId()).isEqualTo(TARGET);
        assertThat(agreement.getContractSigningDate()).isEqualTo(Instant.parse(TIMESTAMP).getEpochSecond());

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void transform_nullPolicy() {
        var value = "example";
        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, value)
                .add(JsonLdKeywords.TYPE, DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE)
                .add(DSPACE_PROPERTY_CONSUMER_PID, "consumerPid")
                .add(DSPACE_PROPERTY_PROVIDER_PID, "providerPid")
                .add(DSPACE_PROPERTY_AGREEMENT, contractAgreement())
                .add(DSPACE_PROPERTY_TIMESTAMP, "123")
                .build();

        when(context.transform(any(JsonObject.class), eq(Policy.class))).thenReturn(null);

        assertThat(transformer.transform(getExpanded(message), context)).isNull();

        verify(context, times(1)).reportProblem(anyString());
    }

    @Test
    void transform_invalidTimestamp() {
        var agreement = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, AGREEMENT_ID)
                .add(JsonLdKeywords.TYPE, ODRL_POLICY_TYPE_AGREEMENT)
                .add(DSPACE_PROPERTY_CONSUMER_ID, CONSUMER_ID)
                .add(DSPACE_PROPERTY_PROVIDER_ID, PROVIDER_ID)
                .add(DSPACE_PROPERTY_TIMESTAMP, "Invalid Timestamp")
                .build();

        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, "messageId")
                .add(JsonLdKeywords.TYPE, DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE)
                .add(DSPACE_PROPERTY_CONSUMER_PID, "consumerPid")
                .add(DSPACE_PROPERTY_PROVIDER_PID, "providerPid")
                .add(DSPACE_PROPERTY_AGREEMENT, agreement)
                .build();

        when(context.transform(any(JsonObject.class), eq(Policy.class))).thenReturn(policy());

        assertThat(transformer.transform(getExpanded(message), context)).isNull();

        verify(context, times(1)).reportProblem(contains(DSPACE_PROPERTY_TIMESTAMP));
    }

    @Test
    void transform_missingTimestamp() {
        var agreement = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, AGREEMENT_ID)
                .add(JsonLdKeywords.TYPE, ODRL_POLICY_TYPE_AGREEMENT)
                .add(DSPACE_PROPERTY_CONSUMER_ID, CONSUMER_ID)
                .add(DSPACE_PROPERTY_PROVIDER_ID, PROVIDER_ID)
                .build();

        var message = jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, "messageId")
                .add(JsonLdKeywords.TYPE, DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE)
                .add(DSPACE_PROPERTY_CONSUMER_PID, "consumerPid")
                .add(DSPACE_PROPERTY_PROVIDER_PID, "providerPid")
                .add(DSPACE_PROPERTY_AGREEMENT, agreement)
                .build();

        when(context.transform(any(JsonObject.class), eq(Policy.class))).thenReturn(policy());

        assertThat(transformer.transform(getExpanded(message), context)).isNull();

        verify(context, times(1)).reportProblem(contains(DSPACE_PROPERTY_TIMESTAMP));
    }

    private JsonObject contractAgreement() {
        return jsonFactory.createObjectBuilder()
                .add(JsonLdKeywords.ID, AGREEMENT_ID)
                .add(JsonLdKeywords.TYPE, ODRL_POLICY_TYPE_AGREEMENT)
                .add(DSPACE_PROPERTY_CONSUMER_ID, CONSUMER_ID)
                .add(DSPACE_PROPERTY_PROVIDER_ID, PROVIDER_ID)
                .add(DSPACE_PROPERTY_TIMESTAMP, TIMESTAMP)
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
                .target(TARGET)
                .build();
    }


}
