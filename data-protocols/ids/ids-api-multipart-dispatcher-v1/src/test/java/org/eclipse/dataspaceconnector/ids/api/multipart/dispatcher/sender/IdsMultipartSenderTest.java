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
 *       Fraunhofer Institute for Software and Systems Engineering - replace object mapper
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.core.serialization.IdsTypeManagerUtil;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdsMultipartSenderTest {
    private final IdentityService identityService = mock(IdentityService.class);

    @Test
    void should_fail_if_token_retrieval_fails() {
        when(identityService.obtainClientCredentials(any())).thenReturn(Result.failure("error"));

        var objectMapper = IdsTypeManagerUtil.getIdsObjectMapper(new TypeManager());

        var sender = new IdsMultipartSender(mock(Monitor.class), mock(OkHttpClient.class), identityService, objectMapper);
        var senderDelegate = mock(MultipartSenderDelegate.class);

        var result = sender.send(new TestRemoteMessage(), senderDelegate);

        assertThat(result).failsWithin(1, TimeUnit.SECONDS);
    }

    private static class TestRemoteMessage implements RemoteMessage {

        @Override
        public String getProtocol() {
            return null;
        }

        @Override
        public String getConnectorAddress() {
            return "some.remote.url";
        }
    }

}
