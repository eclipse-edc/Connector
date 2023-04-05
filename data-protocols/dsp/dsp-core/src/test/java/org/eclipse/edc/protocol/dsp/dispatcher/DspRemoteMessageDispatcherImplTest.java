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

package org.eclipse.edc.protocol.dsp.dispatcher;

import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspRemoteMessageDispatcher;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.protocol.dsp.spi.types.MessageProtocol.DATA_SPACE_PROTOCOL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DspRemoteMessageDispatcherImplTest {
    
    private EdcHttpClient httpClient = mock(EdcHttpClient.class);
    private IdentityService identityService = mock(IdentityService.class);
    private DspDispatcherDelegate<TestMessage, String> delegate = mock(DspDispatcherDelegate.class);
    
    private DspRemoteMessageDispatcher dispatcher;
    
    @BeforeEach
    void setUp() {
        dispatcher = new DspRemoteMessageDispatcherImpl(httpClient, identityService);
        
        when(delegate.getMessageType()).thenReturn(TestMessage.class);
    }
    
    @Test
    void protocol_returnDsp() {
        assertThat(dispatcher.protocol()).isEqualTo(DATA_SPACE_PROTOCOL);
    }
    
    @Test
    void send_sendRequestViaHttpClient() throws ExecutionException, InterruptedException {
        var responseBody = "response";
        Function<Response, String> responseFunction = response -> responseBody;
        var authToken = "token";
        
        when(delegate.buildRequest(any())).thenReturn(new Request.Builder().url("http://url").build());
        when(delegate.parseResponse()).thenReturn(responseFunction);
        when(httpClient.executeAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(responseBody));
        when(identityService.obtainClientCredentials(any()))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token(authToken).build()));
    
        dispatcher.registerDelegate(delegate);
        
        var message = new TestMessage();
        var result = dispatcher.send(String.class, message);
        
        assertThat(result.get()).isEqualTo(responseBody);
        
        verify(delegate).buildRequest(message);
        verify(identityService).obtainClientCredentials(argThat(tr -> tr.getAudience().equals(message.getConnectorAddress())));
        verify(httpClient).executeAsync(argThat(r -> r.headers().get("Authorization").equals(authToken)), eq(responseFunction));
    }
    
    @Test
    void send_noDelegateFound_throwException() {
        assertThatThrownBy(() -> dispatcher.send(String.class, new TestMessage())).isInstanceOf(EdcException.class);
        verifyNoInteractions(httpClient);
    }
    
    @Test
    void send_failedToObtainToken_throwException() {
        when(delegate.buildRequest(any())).thenReturn(new Request.Builder().url("http://url").build());
        when(identityService.obtainClientCredentials(any())).thenReturn(Result.failure("error"));
        
        assertThatThrownBy(() -> dispatcher.send(String.class, new TestMessage())).isInstanceOf(EdcException.class);
        verifyNoInteractions(httpClient);
    }
    
    class TestMessage implements RemoteMessage {
        @Override
        public String getProtocol() {
            return null;
        }
        
        @Override
        public String getConnectorAddress() {
            return "http://connector";
        }
    }
}
