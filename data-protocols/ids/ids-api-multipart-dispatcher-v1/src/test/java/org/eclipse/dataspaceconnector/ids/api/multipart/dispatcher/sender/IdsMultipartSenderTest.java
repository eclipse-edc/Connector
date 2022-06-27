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
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdsMultipartSenderTest {
    private static final Faker FAKER = new Faker();
    private final IdentityService identityService = mock(IdentityService.class);
    private final String remoteAddress = FAKER.internet().url();

    @Test
    void should_fail_if_token_retrieval_fails() {
        when(identityService.obtainClientCredentials(any())).thenReturn(Result.failure("error"));
        var sender = new TestIdsMultipartSender("any", mock(OkHttpClient.class), new ObjectMapper(), mock(Monitor.class), identityService, mock(IdsTransformerRegistry.class));

        var result = sender.send(new TestRemoteMessage(), () -> "any");

        assertThat(result).failsWithin(1, TimeUnit.SECONDS);
    }

    private class TestIdsMultipartSender extends IdsMultipartSender<TestRemoteMessage, Object> {

        protected TestIdsMultipartSender(String connectorId, OkHttpClient httpClient, ObjectMapper objectMapper,
                                         Monitor monitor, IdentityService identityService, IdsTransformerRegistry transformerRegistry) {
            super(connectorId, httpClient, objectMapper, monitor, identityService, transformerRegistry);
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
