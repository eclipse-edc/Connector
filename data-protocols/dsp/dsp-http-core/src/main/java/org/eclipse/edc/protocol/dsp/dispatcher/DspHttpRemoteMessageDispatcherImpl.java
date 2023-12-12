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

import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpRemoteMessageDispatcher;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpRequestFactory;
import org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenDecorator;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.eclipse.edc.spi.http.FallbackFactories.retryWhenStatusNot2xxOr4xx;

/**
 * Dispatches remote messages using the dataspace protocol. Uses {@link DspHttpDispatcherDelegate}s
 * for creating the requests and parsing the responses for specific message types.
 */
public class DspHttpRemoteMessageDispatcherImpl implements DspHttpRemoteMessageDispatcher {

    private final Map<Class<? extends RemoteMessage>, Handlers<?, ?>> handlers = new HashMap<>();
    private final Map<Class<? extends RemoteMessage>, PolicyScope<? extends RemoteMessage>> policyScopes = new HashMap<>();
    private final EdcHttpClient httpClient;
    private final IdentityService identityService;
    private final PolicyEngine policyEngine;
    private final TokenDecorator tokenDecorator;

    public DspHttpRemoteMessageDispatcherImpl(EdcHttpClient httpClient,
                                              IdentityService identityService,
                                              TokenDecorator decorator,
                                              PolicyEngine policyEngine) {
        this.httpClient = httpClient;
        this.identityService = identityService;
        this.policyEngine = policyEngine;
        this.tokenDecorator = decorator;
    }

    @Override
    public String protocol() {
        return HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
    }

    @Override
    public <T, M extends RemoteMessage> CompletableFuture<StatusResult<T>> dispatch(Class<T> responseType, M message) {
        var handlers = (Handlers<M, T>) this.handlers.get(message.getClass());
        if (handlers == null) {
            return failedFuture(new EdcException(format("No DSP message dispatcher found for message type %s", message.getClass())));
        }

        var request = handlers.requestFactory.createRequest(message);

        var tokenParametersBuilder = tokenDecorator.decorate(TokenParameters.Builder.newInstance());

        var policyScope = policyScopes.get(message.getClass());
        if (policyScope != null) {
            var context = PolicyContextImpl.Builder.newInstance()
                    .additional(TokenParameters.Builder.class, tokenParametersBuilder)
                    .build();
            var policyProvider = (Function<M, Policy>) policyScope.policyProvider;
            policyEngine.evaluate(policyScope.scope, policyProvider.apply(message), context);
        }

        var tokenParameters = tokenParametersBuilder
                .audience(message.getCounterPartyAddress()) // enforce the audience, ignore anything a decorator might have set
                .build();

        return identityService.obtainClientCredentials(tokenParameters)
                .map(token -> {
                    var requestWithAuth = request.newBuilder()
                            .header("Authorization", token.getToken())
                            .build();

                    return httpClient.executeAsync(requestWithAuth, List.of(retryWhenStatusNot2xxOr4xx()), handlers.delegate.handleResponse());
                })
                .orElse(failure -> failedFuture(new EdcException(format("Unable to obtain credentials: %s", failure.getFailureDetail()))));
    }

    @Override
    public <M extends RemoteMessage> void registerPolicyScope(Class<M> messageClass, String scope, Function<M, Policy> policyProvider) {
        policyScopes.put(messageClass, new PolicyScope<>(messageClass, scope, policyProvider));
    }

    @Override
    public <M extends RemoteMessage, R> void registerMessage(Class<M> clazz, DspHttpRequestFactory<M> requestFactory, DspHttpDispatcherDelegate<R> delegate) {
        handlers.put(clazz, new Handlers<>(requestFactory, delegate));
    }

    private record Handlers<M extends RemoteMessage, R>(DspHttpRequestFactory<M> requestFactory, DspHttpDispatcherDelegate<R> delegate) { }

    private record PolicyScope<M extends RemoteMessage>(Class<M> messageClass, String scope, Function<M, Policy> policyProvider) {}

}
