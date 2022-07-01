/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import jakarta.ws.rs.core.MediaType;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdsMultipartSenderTest {
    private static final Faker FAKER = new Faker();
    private final IdentityService identityService = mock(IdentityService.class);
    private final String remoteAddress = "http://localhost:8282/api/v1/ids/data";

    @Test
    void shouldSend() throws IOException {
        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token("token").additional(Map.of("rights", "catalog1")).build();
        var httpClient = mock(OkHttpClient.class);
        var requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
        var futureCallbackArgumentCaptor = ArgumentCaptor.forClass(Callback.class);
        var call = mock(Call.class);
        var httpRequest = new Request.Builder()
                .url(remoteAddress)
                .addHeader("Content-Type", MediaType.MULTIPART_FORM_DATA)
                .post(RequestBody.create("text", okhttp3.MediaType.get(MediaType.TEXT_PLAIN)))
                .build();
        var responseOk = new Response.Builder()
                .request(httpRequest)
                .message("OK")
                .protocol(Protocol.HTTP_1_1)
                .code(200).build();

        doNothing().when(call).enqueue(futureCallbackArgumentCaptor.capture());
        when(identityService.obtainClientCredentials(any(), any())).thenReturn(Result.success(tokenRepresentation));
        when(httpClient.newCall(requestArgumentCaptor.capture())).thenReturn(call);

        var sender = new TestIdsMultipartSender("any", httpClient, new ObjectMapper(), mock(Monitor.class), identityService, mock(IdsTransformerRegistry.class));

        var result = sender.send(new TestRemoteMessage(), () -> null);

        futureCallbackArgumentCaptor.getValue().onResponse(mock(Call.class), responseOk);

        assertThat(result).succeedsWithin(1, TimeUnit.SECONDS);
        assertThat(requestArgumentCaptor.getValue().body()).isNotNull();
    }

    @Test
    void should_fail_if_token_retrieval_fails() {
        when(identityService.obtainClientCredentials(any(), any())).thenReturn(Result.failure("error"));
        var sender = new TestIdsMultipartSender("any", mock(OkHttpClient.class), new ObjectMapper(), mock(Monitor.class), identityService, mock(IdsTransformerRegistry.class));

        var result = sender.send(new TestRemoteMessage(), () -> "any");

        assertThat(result).failsWithin(1, TimeUnit.SECONDS);
    }

    private class TestIdsMultipartSender extends IdsMultipartSender<TestRemoteMessage, Object> {

        protected TestIdsMultipartSender(String connectorId, OkHttpClient httpClient, ObjectMapper objectMapper,
                                         Monitor monitor, IdentityService identityService, IdsTransformerRegistry transformerRegistry) {
            super(connectorId, httpClient, objectMapper, monitor, identityService, transformerRegistry, context -> context.getFuture().complete(null));
        }

        @Override
        public Class<TestRemoteMessage> messageType() {
            return null;
        }

        @Override
        protected String retrieveRemoteConnectorAddress(TestRemoteMessage request) {
            return remoteAddress;
        }

        @Override
        protected Message buildMessageHeader(TestRemoteMessage request, DynamicAttributeToken token) {
            return null;
        }

        @Override
        protected Object getResponseContent(IdsMultipartParts parts) {
            return null;
        }
    }

    private static class TestRemoteMessage implements RemoteMessage {

        @Override
        public String getProtocol() {
            return null;
        }
    }
}
