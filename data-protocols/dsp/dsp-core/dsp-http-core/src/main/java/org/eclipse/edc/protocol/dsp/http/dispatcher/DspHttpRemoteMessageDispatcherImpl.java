/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.http.dispatcher;

import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.dsp.http.spi.dispatcher.DspHttpRemoteMessageDispatcher;
import org.eclipse.edc.protocol.dsp.http.spi.dispatcher.DspHttpRequestFactory;
import org.eclipse.edc.protocol.dsp.http.spi.dispatcher.response.DspHttpResponseBodyExtractor;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.RequestContext;
import org.eclipse.edc.spi.iam.RequestScope;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.message.ProtocolRemoteMessage;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.eclipse.edc.http.spi.FallbackFactories.retryWhenStatusNot2xxOr4xx;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Dispatches remote messages using the dataspace protocol.
 */
public class DspHttpRemoteMessageDispatcherImpl implements DspHttpRemoteMessageDispatcher {

    private static final String AUDIENCE_CLAIM = "aud";
    private static final String SCOPE_CLAIM = "scope";
    private final Map<Class<? extends RemoteMessage>, MessageHandler<?, ?>> handlers = new HashMap<>();
    private final Map<Class<? extends RemoteMessage>, PolicyScope<? extends RemoteMessage>> policyScopes = new HashMap<>();
    private final EdcHttpClient httpClient;
    private final IdentityService identityService;
    private final PolicyEngine policyEngine;
    private final TokenDecorator tokenDecorator;
    private final AudienceResolver audienceResolver;


    public DspHttpRemoteMessageDispatcherImpl(EdcHttpClient httpClient,
                                              IdentityService identityService,
                                              TokenDecorator decorator,
                                              PolicyEngine policyEngine,
                                              AudienceResolver audienceResolver) {
        this.httpClient = httpClient;
        this.identityService = identityService;
        this.policyEngine = policyEngine;
        this.tokenDecorator = decorator;
        this.audienceResolver = audienceResolver;
    }

    @Override
    public <T, M extends ProtocolRemoteMessage> CompletableFuture<StatusResult<T>> dispatch(String participantContextId, Class<T> responseType, M message) {
        var handler = (MessageHandler<M, T>) this.handlers.get(message.getClass());
        if (handler == null) {
            return failedFuture(new EdcException(format("No DSP message dispatcher found for message type %s", message.getClass())));
        }

        var request = handler.requestFactory.createRequest(message);

        var tokenParametersBuilder = TokenParameters.Builder.newInstance();

        var policyScope = policyScopes.get(message.getClass());
        if (policyScope != null) {
            var requestScopeBuilder = RequestScope.Builder.newInstance();
            var requestContext = RequestContext.Builder.newInstance()
                    .message(message)
                    .direction(RequestContext.Direction.Egress)
                    .build();

            var context = policyScope.contextProvider.instantiate(requestContext, requestScopeBuilder);
            var policyProvider = (Function<M, Policy>) policyScope.policyProvider;
            policyEngine.evaluate(policyProvider.apply(message), context);

            // catalog request messages can carry additional, user-supplied scopes
            if (message instanceof CatalogRequestMessage catalogRequestMessage) {
                catalogRequestMessage.getAdditionalScopes().forEach(requestScopeBuilder::scope);
            }

            var scopes = requestScopeBuilder.build().getScopes();

            // Only add the scope claim if there are scopes returned from the policy engine evaluation
            if (!scopes.isEmpty()) {
                tokenParametersBuilder.claims(SCOPE_CLAIM, String.join(" ", scopes));
            }
        }

        return audienceResolver.resolve(message)
                .map(audience -> tokenDecorator.decorate(tokenParametersBuilder).claims(AUDIENCE_CLAIM, audience).build()) // enforce the audience, ignore anything a decorator might have set
                .compose(token -> identityService.obtainClientCredentials(participantContextId, token))
                .map(token -> {
                    var requestWithAuth = request.newBuilder()
                            .header("Authorization", token.getToken())
                            .build();

                    return httpClient.executeAsync(requestWithAuth, List.of(retryWhenStatusNot2xxOr4xx()))
                            .thenApply(response -> handleResponse(response, message.getProtocol(), responseType, handler.bodyExtractor));
                })
                .orElse(failure -> failedFuture(new EdcException(format("Unable to obtain credentials: %s", failure.getFailureDetail()))));
    }

    @Override
    public <M extends ProtocolRemoteMessage, R> void registerMessage(Class<M> clazz, DspHttpRequestFactory<M> requestFactory,
                                                             DspHttpResponseBodyExtractor<R> bodyExtractor) {
        handlers.put(clazz, new MessageHandler<>(requestFactory, bodyExtractor));
    }

    @Override
    public <M extends ProtocolRemoteMessage> void registerPolicyScope(Class<M> messageClass,
                                                              Function<M, Policy> policyProvider,
                                                              RequestPolicyContext.Provider contextProvider) {
        policyScopes.put(messageClass, new PolicyScope<>(messageClass, policyProvider, contextProvider));
    }

    @NotNull
    private <T> StatusResult<T> handleResponse(Response response, String protocol, Class<T> responseType, DspHttpResponseBodyExtractor<T> bodyExtractor) {
        try (var responseBody = response.body()) {
            if (response.isSuccessful()) {
                var responsePayload = bodyExtractor.extractBody(responseBody, protocol);

                return StatusResult.success(responseType.cast(responsePayload));
            } else {
                var status = response.code() >= 400 && response.code() < 500 ? FATAL_ERROR : ERROR_RETRY;

                return StatusResult.failure(status, asString(responseBody));
            }
        }
    }

    private String asString(ResponseBody it) {
        try {
            return it.string();
        } catch (IOException e) {
            return "Cannot read response body: " + e.getMessage();
        }
    }

    private record MessageHandler<M extends RemoteMessage, R>(
            DspHttpRequestFactory<M> requestFactory,
            DspHttpResponseBodyExtractor<R> bodyExtractor) {
    }

    private record PolicyScope<M extends RemoteMessage>(
            Class<M> messageClass,
            Function<M, Policy> policyProvider,
            RequestPolicyContext.Provider contextProvider) {
    }

}
