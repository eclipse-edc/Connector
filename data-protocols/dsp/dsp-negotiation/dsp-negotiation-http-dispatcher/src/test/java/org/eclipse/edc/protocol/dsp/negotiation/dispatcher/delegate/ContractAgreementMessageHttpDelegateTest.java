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

import jakarta.json.JsonObject;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.protocol.dsp.spi.serialization.JsonLdRemoteMessageSerializer;
import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.BASE_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractAgreementMessageHttpDelegateTest {

    private final JsonLdRemoteMessageSerializer serializer = mock(JsonLdRemoteMessageSerializer.class);

    private ContractAgreementMessageHttpDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new ContractAgreementMessageHttpDelegate(serializer);
    }

    @Test
    void getMessageType() {
        assertThat(delegate.getMessageType()).isEqualTo(ContractAgreementMessage.class);
    }

    @Test
    void buildRequest() throws IOException {
        var message = message();
        var serializedBody = "message";

        when(serializer.serialize(eq(message), any(JsonObject.class))).thenReturn(serializedBody);
        
        var httpRequest = delegate.buildRequest(message);

        assertThat(httpRequest.url().url()).hasToString(message.getCallbackAddress() + BASE_PATH + message.getProcessId() + AGREEMENT);
        assertThat(readRequestBody(httpRequest)).isEqualTo(serializedBody);

        verify(serializer, times(1)).serialize(eq(message), any(JsonObject.class));
    }

    @Test
    void buildRequest_serializationFails_throwException() {
        var message = message();
        
        when(serializer.serialize(eq(message), any(JsonObject.class))).thenThrow(EdcException.class);

        assertThatThrownBy(() -> delegate.buildRequest(message)).isInstanceOf(EdcException.class);
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