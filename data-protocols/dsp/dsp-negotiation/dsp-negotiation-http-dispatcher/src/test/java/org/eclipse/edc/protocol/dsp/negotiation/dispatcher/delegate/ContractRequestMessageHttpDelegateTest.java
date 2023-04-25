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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.testfixtures.dispatcher.DspHttpDispatcherDelegateTestBase;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.expand;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_CONTRACT_NEGOTIATION;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_CHECKSUM;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_PROPERTY_STATE;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.DspNegotiationPropertyAndTypeNames.DSPACE_NEGOTIATION_STATE_REQUESTED;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.spi.NegotiationApiPaths.INITIAL_CONTRACT_REQUEST;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractRequestMessageHttpDelegateTest extends DspHttpDispatcherDelegateTestBase<ContractRequestMessage> {

    private ContractRequestMessageHttpDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new ContractRequestMessageHttpDelegate(serializer, mapper);
    }

    @Test
    void getMessageType() {
        assertThat(delegate.getMessageType()).isEqualTo(ContractRequestMessage.class);
    }

    @Test
    void buildRequest_initial() throws IOException {
        testBuildRequest_shouldReturnRequest(message_initial(), BASE_PATH + INITIAL_CONTRACT_REQUEST);
    }

    @Test
    void buildRequest() throws IOException {
        var message = message();
        testBuildRequest_shouldReturnRequest(message, BASE_PATH + message.getProcessId() + CONTRACT_REQUEST);
    }

    @Test
    void buildRequest_serializationFails_throwException() {
        testBuildRequest_shouldThrowException_whenSerializationFails(message());
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
        testParseResponse_shouldThrowException_whenResponseBodyNull();
    }
    
    @Test
    void parseResponse_readingResponseBodyFails_throwException() throws IOException {
        testParseResponse_shouldThrowException_whenReadingResponseBodyFails();
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
    
    @Override
    protected DspHttpDispatcherDelegate<ContractRequestMessage, ?> delegate() {
        return delegate;
    }
}