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

package org.eclipse.edc.protocol.dsp.negotiation.dispatcher.delegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.BASE_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractAgreementMessageHttpDelegateTest {

    private final ObjectMapper mapper = mock(ObjectMapper.class);
    private final JsonLdTransformerRegistry registry = mock(JsonLdTransformerRegistry.class);

    private ContractAgreementMessageHttpDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new ContractAgreementMessageHttpDelegate(mapper, registry);
    }

    @Test
    void getMessageType() {
        assertThat(delegate.getMessageType()).isEqualTo(ContractAgreementMessage.class);
    }

    @Test
    void buildRequest() throws IOException {
        var jsonObject = Json.createObjectBuilder().add(DSPACE_SCHEMA + "key", "value").build();
        var serializedBody = "message";

        when(registry.transform(isA(ContractAgreementMessage.class), eq(JsonObject.class))).thenReturn(Result.success(jsonObject));
        when(mapper.writeValueAsString(any(JsonObject.class))).thenReturn(serializedBody);

        var message = message();
        var httpRequest = delegate.buildRequest(message);

        assertThat(httpRequest.url().url()).hasToString(message.getCallbackAddress() + BASE_PATH + message.getProcessId() + AGREEMENT);
        assertThat(readRequestBody(httpRequest)).isEqualTo(serializedBody);

        verify(registry, times(1)).transform(any(ContractAgreementMessage.class), eq(JsonObject.class));
        verify(mapper, times(1)).writeValueAsString(argThat(json -> ((JsonObject) json).get(CONTEXT) != null && ((JsonObject) json).get(DSPACE_PREFIX + ":key") != null));
    }

    @Test
    void buildRequest_transformationFails_throwException() {
        when(registry.transform(any(ContractAgreementMessage.class), eq(JsonObject.class))).thenReturn(Result.failure("error"));

        assertThatThrownBy(() -> delegate.buildRequest(message())).isInstanceOf(EdcException.class);
    }

    @Test
    void buildRequest_writingJsonFails_throwException() throws JsonProcessingException {
        when(registry.transform(any(ContractAgreementMessage.class), eq(JsonObject.class))).thenReturn(Result.success(Json.createObjectBuilder().build()));
        when(mapper.writeValueAsString(any(JsonObject.class))).thenThrow(JsonProcessingException.class);

        assertThatThrownBy(() -> delegate.buildRequest(message())).isInstanceOf(EdcException.class);
    }

    @Test
    void parseResponse_returnNull() {
        var response = mock(Response.class);

        assertThat(delegate.parseResponse().apply(response)).isNull();
    }

    private ContractAgreementMessage message() {
        var value = "example";
        return ContractAgreementMessage.Builder.newInstance()
                .protocol(value)
                .processId(value)
                .callbackAddress("http://connector")
                .contractAgreement(contractAgreement())
                .build();
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

    private String readRequestBody(Request request) throws IOException {
        var buffer = new Buffer();
        request.body().writeTo(buffer);
        return buffer.readUtf8();
    }
}