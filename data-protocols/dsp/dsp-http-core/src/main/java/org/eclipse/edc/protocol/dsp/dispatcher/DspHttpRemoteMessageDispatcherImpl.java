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

import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpRemoteMessageDispatcher;
import org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;

/**
 * Dispatches remote messages using the dataspace protocol. Uses {@link DspHttpDispatcherDelegate}s
 * for creating the requests and parsing the responses for specific message types.
 */
public class DspHttpRemoteMessageDispatcherImpl implements DspHttpRemoteMessageDispatcher {

    private Map<Class<? extends RemoteMessage>, DspHttpDispatcherDelegate> delegates;
    private EdcHttpClient httpClient;
    private IdentityService identityService;

    public DspHttpRemoteMessageDispatcherImpl(EdcHttpClient httpClient, IdentityService identityService) {
        this.httpClient = httpClient;
        this.identityService = identityService;
        this.delegates = new HashMap<>();
    }

    @Override
    public String protocol() {
        return HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
    }

    @Override
    public <M extends RemoteMessage, R> void registerDelegate(DspHttpDispatcherDelegate<M, R> delegate) {
        delegates.put(delegate.getMessageType(), delegate);
    }

    /**
     * Sends a remote message. Chooses the delegate for the respective message type to build the
     * request. Adds the token received from the {@link IdentityService} as the Authorization header.
     * Sends the request using the {@link EdcHttpClient} and parses the response as defined by
     * the delegate.
     *
     * @param responseType the expected response type
     * @param message the message
     * @return a completable future for the response body
     */
    @Override
    public <T, M extends RemoteMessage> CompletableFuture<T> send(Class<T> responseType, M message) {
        var delegate = (DspHttpDispatcherDelegate<M, T>) delegates.get(message.getClass());
        if (delegate == null) {
            throw new EdcException(format("No DSP message dispatcher found for message type %s", message.getClass()));
        }

        var request = delegate.buildRequest(message);

        var token = obtainToken(message);
        var requestWithAuth = request.newBuilder()
                .header("Authorization", token.getToken())
                .build();

        return httpClient.executeAsync(requestWithAuth, delegate.parseResponse());
    }

    private TokenRepresentation obtainToken(RemoteMessage message) {
        var tokenParameters = TokenParameters.Builder.newInstance()
                .audience(message.getCallbackAddress())
                .build();
        var tokenResult = identityService.obtainClientCredentials(tokenParameters);
        if (tokenResult.failed()) {
            throw new EdcException(format("Unable to obtain credentials: %s", String.join(", ", tokenResult.getFailureMessages())));
        }

        return tokenResult.getContent();
    }
}
