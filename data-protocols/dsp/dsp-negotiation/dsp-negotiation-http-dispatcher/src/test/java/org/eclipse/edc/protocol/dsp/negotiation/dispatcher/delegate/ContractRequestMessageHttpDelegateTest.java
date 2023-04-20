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
import okhttp3.ResponseBody;
import okio.Buffer;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.expand;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_CONTRACT_NEGOTIATION;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CHECKSUM;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_STATE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_STATE_REQUESTED;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.INITIAL_CONTRACT_REQUEST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractRequestMessageHttpDelegateTest {

    private final ObjectMapper mapper = mock(ObjectMapper.class);
    private final JsonLdTransformerRegistry registry = mock(JsonLdTransformerRegistry.class);

    private ContractRequestMessageHttpDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new ContractRequestMessageHttpDelegate(mapper, registry);
    }

    @Test
    void getMessageType() {
        assertThat(delegate.getMessageType()).isEqualTo(ContractRequestMessage.class);
    }

    @Test
    void buildRequest_initial() throws IOException {
        var jsonObject = Json.createObjectBuilder().add(DSPACE_SCHEMA + "key", "value").build();
        var serializedBody = "message";

        when(registry.transform(isA(ContractRequestMessage.class), eq(JsonObject.class))).thenReturn(Result.success(jsonObject));
        when(mapper.writeValueAsString(any(JsonObject.class))).thenReturn(serializedBody);

        var message = message_initial();
        var httpRequest = delegate.buildRequest(message);

        assertThat(httpRequest.url().url()).hasToString(message.getCallbackAddress() + BASE_PATH + INITIAL_CONTRACT_REQUEST);
        assertThat(readRequestBody(httpRequest)).isEqualTo(serializedBody);

        verify(registry, times(1)).transform(any(ContractRequestMessage.class), eq(JsonObject.class));
        verify(mapper, times(1)).writeValueAsString(argThat(json -> ((JsonObject) json).get(CONTEXT) != null && ((JsonObject) json).get(DSPACE_PREFIX + ":key") != null));
    }

    @Test
    void buildRequest() throws IOException {
        var jsonObject = Json.createObjectBuilder().add(DSPACE_SCHEMA + "key", "value").build();
        var serializedBody = "message";

        when(registry.transform(isA(ContractRequestMessage.class), eq(JsonObject.class))).thenReturn(Result.success(jsonObject));
        when(mapper.writeValueAsString(any(JsonObject.class))).thenReturn(serializedBody);

        var message = message();
        var httpRequest = delegate.buildRequest(message);

        assertThat(httpRequest.url().url()).hasToString(message.getCallbackAddress() + BASE_PATH + message.getProcessId() + CONTRACT_REQUEST);
        assertThat(readRequestBody(httpRequest)).isEqualTo(serializedBody);

        verify(registry, times(1)).transform(any(ContractRequestMessage.class), eq(JsonObject.class));
        verify(mapper, times(1)).writeValueAsString(argThat(json -> ((JsonObject) json).get(CONTEXT) != null && ((JsonObject) json).get(DSPACE_PREFIX + ":key") != null));
    }

    @Test
    void buildRequest_transformationFails_throwException() {
        when(registry.transform(any(ContractRequestMessage.class), eq(JsonObject.class))).thenReturn(Result.failure("error"));

        assertThatThrownBy(() -> delegate.buildRequest(message())).isInstanceOf(EdcException.class);
    }

    @Test
    void buildRequest_writingJsonFails_throwException() throws JsonProcessingException {
        when(registry.transform(any(ContractRequestMessage.class), eq(JsonObject.class))).thenReturn(Result.success(Json.createObjectBuilder().build()));
        when(mapper.writeValueAsString(any(JsonObject.class))).thenThrow(JsonProcessingException.class);

        assertThatThrownBy(() -> delegate.buildRequest(message())).isInstanceOf(EdcException.class);
    }

    @Test
    void parseResponse_returnNegotiation() throws IOException {
        var response = mock(Response.class);
        var responseBody = mock(ResponseBody.class);
        var bytes = "test".getBytes();

        var expanded = expand(negotiation()).get(0);

        when(response.body()).thenReturn(responseBody);
        when(responseBody.bytes()).thenReturn(bytes);
        when(mapper.readValue(bytes, JsonObject.class)).thenReturn(negotiation());

        var result = delegate.parseResponse().apply(response);

        assertThat(result).isEqualTo(expanded);
        verify(mapper, times(1)).readValue(bytes, JsonObject.class);
    }

    @Test
    void parseResponse_responseBodyNull_throwException() {
        var response = mock(Response.class);

        when(response.body()).thenReturn(null);

        assertThatThrownBy(() -> delegate.parseResponse().apply(response)).isInstanceOf(EdcException.class);
    }

    private ContractRequestMessage message() {
        var value = "example";
        return ContractRequestMessage.Builder.newInstance()
                .protocol(value)
                .processId(value)
                .callbackAddress("http://connector")
                .dataSet(value)
                .contractOffer(contractOffer())
                .type(ContractRequestMessage.Type.COUNTER_OFFER)
                .build();
    }

    private ContractRequestMessage message_initial() {
        var value = "example";
        return ContractRequestMessage.Builder.newInstance()
                .protocol(value)
                .processId(value)
                .callbackAddress("http://connector")
                .dataSet(value)
                .contractOffer(contractOffer())
                .type(ContractRequestMessage.Type.INITIAL)
                .build();
    }

    private ContractOffer contractOffer() {
        return ContractOffer.Builder.newInstance()
                .id(String.valueOf(UUID.randomUUID()))
                .asset(Asset.Builder.newInstance().id("assetId").build())
                .contractStart(ZonedDateTime.now())
                .contractEnd(ZonedDateTime.now())
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

    private JsonObject negotiation() {
        var value = "example";
        var builder = Json.createObjectBuilder();
        builder.add(JsonLdKeywords.ID, value);
        builder.add(JsonLdKeywords.TYPE, DSPACE_CONTRACT_NEGOTIATION);

        builder.add(DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID, value);
        builder.add(DSPACE_NEGOTIATION_PROPERTY_STATE, DSPACE_NEGOTIATION_STATE_REQUESTED);
        builder.add(DSPACE_NEGOTIATION_PROPERTY_CHECKSUM, value);

        return builder.build();
    }

    private String readRequestBody(Request request) throws IOException {
        var buffer = new Buffer();
        request.body().writeTo(buffer);
        return buffer.readUtf8();
    }
}