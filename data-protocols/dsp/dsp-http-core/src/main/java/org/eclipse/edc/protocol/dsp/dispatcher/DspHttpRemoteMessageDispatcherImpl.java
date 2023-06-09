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

import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpRemoteMessageDispatcher;
import org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenDecorator;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.eclipse.edc.spi.http.FallbackFactories.statusMustBeSuccessful;

/**
 * Dispatches remote messages using the dataspace protocol. Uses {@link DspHttpDispatcherDelegate}s
 * for creating the requests and parsing the responses for specific message types.
 */
public class DspHttpRemoteMessageDispatcherImpl implements DspHttpRemoteMessageDispatcher {

    private final Map<Class<? extends RemoteMessage>, DspHttpDispatcherDelegate<?, ?>> delegates = new HashMap<>();
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

    /**
     * Sends a remote message. Chooses the delegate for the respective message type to build the
     * request. Adds the token received from the {@link IdentityService} as the Authorization header.
     * Sends the request using the {@link EdcHttpClient} and parses the response as defined by
     * the delegate.
     *
     * @param responseType the expected response type
     * @param message      the message
     * @return a completable future for the response body
     */
    @Override
    public <T, M extends RemoteMessage> CompletableFuture<T> send(Class<T> responseType, M message) {
        var delegate = (DspHttpDispatcherDelegate<M, T>) delegates.get(message.getClass());
        if (delegate == null) {
            return failedFuture(new EdcException(format("No DSP message dispatcher found for message type %s", message.getClass())));
        }

        var request = delegate.buildRequest(message);

        var tokenParametersBuilder = tokenDecorator.decorate(TokenParameters.Builder.newInstance());

        var policyScope = policyScopes.get(message.getClass());
        if (policyScope != null) {
            var policyContext = new HashMap<Class<?>, Object>();
            policyContext.put(TokenParameters.Builder.class, tokenParametersBuilder);
            var policyProvider = (Function<M, Policy>) policyScope.policyProvider;
            policyEngine.evaluate(policyScope.scope, policyProvider.apply(message), null, policyContext);
        }

        var tokenParameters = tokenParametersBuilder
                .audience(message.getCounterPartyAddress()) // enforce the audience, ignore anything a decorator might have set
                .build();

        return identityService.obtainClientCredentials(tokenParameters)
                .map(token -> {
                    var requestWithAuth = request.newBuilder()
                            .header("Authorization", token.getToken())
                            .build();

                    return httpClient.executeAsync(requestWithAuth, List.of(statusMustBeSuccessful()), delegate.parseResponse());
                })
                .orElse(failure -> failedFuture(new EdcException(format("Unable to obtain credentials: %s", failure.getFailureDetail()))));
    }

    @Override
    public <M extends RemoteMessage, R> void registerDelegate(DspHttpDispatcherDelegate<M, R> delegate) {
        delegates.put(delegate.getMessageType(), delegate);
    }

    @Override
    public <M extends RemoteMessage> void registerPolicyScope(Class<M> messageClass, String scope, Function<M, Policy> policyProvider) {
        policyScopes.put(messageClass, new PolicyScope<M>(messageClass, scope, policyProvider));
    }

    private record PolicyScope<M extends RemoteMessage>(Class<M> messageClass, String scope, Function<M, Policy> policyProvider) {}

}
