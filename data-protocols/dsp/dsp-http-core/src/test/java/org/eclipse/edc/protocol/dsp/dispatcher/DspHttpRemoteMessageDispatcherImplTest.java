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
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpRemoteMessageDispatcher;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpRequestFactory;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenDecorator;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DspHttpRemoteMessageDispatcherImplTest {

    private final EdcHttpClient httpClient = mock();
    private final IdentityService identityService = mock();
    private final PolicyEngine policyEngine = mock();
    private final TokenDecorator tokenDecorator = mock();
    private final DspHttpRequestFactory<TestMessage> requestFactory = mock();
    private final DspHttpDispatcherDelegate<String> delegate = mock();
    private final Duration timeout = Duration.of(5, SECONDS);

    private DspHttpRemoteMessageDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        when(tokenDecorator.decorate(any())).thenAnswer(a -> a.getArgument(0));
        dispatcher = new DspHttpRemoteMessageDispatcherImpl(httpClient, identityService, tokenDecorator, policyEngine);
    }

    @Test
    void protocol_returnDsp() {
        assertThat(dispatcher.protocol()).isEqualTo(DATASPACE_PROTOCOL_HTTP);
    }

    @Test
    void dispatch_sendRequestViaHttpClient() {
        var responseBody = "response";
        Function<Response, StatusResult<String>> responseFunction = response -> StatusResult.success(responseBody);
        var authToken = "token";

        when(requestFactory.createRequest(any())).thenReturn(new Request.Builder().url("http://url").build());
        when(delegate.handleResponse()).thenReturn(responseFunction);
        when(httpClient.executeAsync(any(), any(), any())).thenReturn(completedFuture(responseBody));
        when(identityService.obtainClientCredentials(any()))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token(authToken).build()));

        dispatcher.registerMessage(TestMessage.class, requestFactory, delegate);

        var message = new TestMessage();
        var result = dispatcher.dispatch(String.class, message);

        assertThat(result).succeedsWithin(timeout).isEqualTo(responseBody);

        verify(requestFactory).createRequest(message);
        verify(identityService).obtainClientCredentials(argThat(tr -> tr.getAudience().equals(message.getCounterPartyAddress())));
        verify(httpClient).executeAsync(argThat(r -> authToken.equals(r.headers().get("Authorization"))), any(), eq(responseFunction));
    }

    @Test
    void dispatch_ensureTokenDecoratorScope() {
        var responseBody = "response";
        Function<Response, StatusResult<String>> responseFunction = response -> StatusResult.success(responseBody);
        var authToken = "token";

        Map<String, Object> additional = Map.of("foo", "bar");

        when(tokenDecorator.decorate(any())).thenAnswer(a -> a.getArgument(0, TokenParameters.Builder.class).scope("test-scope").additional(additional));
        when(requestFactory.createRequest(any())).thenReturn(new Request.Builder().url("http://url").build());
        when(delegate.handleResponse()).thenReturn(responseFunction);
        when(httpClient.executeAsync(any(), any(), any())).thenReturn(completedFuture(responseBody));
        when(identityService.obtainClientCredentials(any()))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token(authToken).build()));

        dispatcher.registerMessage(TestMessage.class, requestFactory, delegate);

        var message = new TestMessage();
        var result = dispatcher.dispatch(String.class, message);

        assertThat(result).succeedsWithin(timeout);

        var captor = ArgumentCaptor.forClass(TokenParameters.class);
        verify(identityService).obtainClientCredentials(captor.capture());
        verify(httpClient).executeAsync(argThat(r -> authToken.equals(r.headers().get("Authorization"))), any(), eq(responseFunction));

        assertThat(captor.getValue()).satisfies(tr -> {
            assertThat(tr.getScope()).isEqualTo("test-scope");
            assertThat(tr.getAudience()).isEqualTo(message.getCounterPartyAddress());
            assertThat(tr.getAdditional()).containsAllEntriesOf(additional);
        });

    }

    @Test
    void dispatch_noDelegateFound_throwException() {
        assertThat(dispatcher.dispatch(String.class, new TestMessage())).failsWithin(timeout)
                .withThrowableThat().withCauseInstanceOf(EdcException.class).withMessageContaining("found");

        verifyNoInteractions(httpClient);
    }

    @Test
    void dispatch_failedToObtainToken_throwException() {
        dispatcher.registerMessage(TestMessage.class, requestFactory, delegate);
        when(requestFactory.createRequest(any())).thenReturn(new Request.Builder().url("http://url").build());
        when(identityService.obtainClientCredentials(any())).thenReturn(Result.failure("error"));

        assertThat(dispatcher.dispatch(String.class, new TestMessage())).failsWithin(timeout)
                .withThrowableThat().withCauseInstanceOf(EdcException.class).withMessageContaining("credentials");

        verifyNoInteractions(httpClient);
    }

    @Test
    void dispatch_shouldNotEvaluatePolicy_whenItIsNotRegistered() {
        when(requestFactory.createRequest(any())).thenReturn(new Request.Builder().url("http://url").build());
        when(delegate.handleResponse()).thenReturn(response -> null);
        when(httpClient.executeAsync(any(), any(), any())).thenReturn(completedFuture(null));
        when(identityService.obtainClientCredentials(any()))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("any").build()));
        dispatcher.registerMessage(TestMessage.class, requestFactory, delegate);

        var result = dispatcher.dispatch(String.class, new TestMessage());

        assertThat(result).succeedsWithin(timeout);
        verifyNoInteractions(policyEngine);
    }

    @Test
    void dispatch_shouldEvaluatePolicy() {
        when(requestFactory.createRequest(any())).thenReturn(new Request.Builder().url("http://url").build());
        when(delegate.handleResponse()).thenReturn(response -> null);
        when(httpClient.executeAsync(any(), any(), any())).thenReturn(completedFuture(null));
        when(identityService.obtainClientCredentials(any()))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("any").build()));
        dispatcher.registerMessage(TestMessage.class, requestFactory, delegate);
        var policy = Policy.Builder.newInstance().build();
        dispatcher.registerPolicyScope(TestMessage.class, "test.message", m -> policy);

        var result = dispatcher.dispatch(String.class, new TestMessage());

        assertThat(result).succeedsWithin(timeout);
        verify(policyEngine).evaluate(eq("test.message"), eq(policy), and(isA(PolicyContext.class), argThat(c -> c.getContextData(TokenParameters.Builder.class) != null)));
    }

    static class TestMessage implements RemoteMessage {
        @Override
        public String getProtocol() {
            return null;
        }

        @Override
        public String getCounterPartyAddress() {
            return "http://connector";
        }
    }
}
