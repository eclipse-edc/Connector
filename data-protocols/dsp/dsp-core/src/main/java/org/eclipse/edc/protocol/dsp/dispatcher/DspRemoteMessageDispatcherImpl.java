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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspRemoteMessageDispatcher;
import org.eclipse.edc.protocol.dsp.spi.types.MessageProtocol;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import static java.lang.String.format;

public class DspRemoteMessageDispatcherImpl implements DspRemoteMessageDispatcher {
    
    private Map<Class<? extends RemoteMessage>, DspDispatcherDelegate> delegates;
    private EdcHttpClient httpClient;
    private IdentityService identityService;
    
    public DspRemoteMessageDispatcherImpl(EdcHttpClient httpClient, IdentityService identityService) {
        this.httpClient = httpClient;
        this.identityService = identityService;
        this.delegates = new HashMap<>();
    }
    
    @Override
    public String protocol() {
        return MessageProtocol.DATA_SPACE_PROTOCOL;
    }
    
    @Override
    public <M extends RemoteMessage, R> void registerDelegate(DspDispatcherDelegate<M, R> delegate) {
        delegates.put(delegate.getMessageType(), delegate);
    }
    
    @Override
    public <T, M extends RemoteMessage> CompletableFuture<T> send(Class<T> responseType, M message) {
        var delegate = (DspDispatcherDelegate<M, T>) delegates.get(message.getClass());
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
                .audience(message.getConnectorAddress())
                .build();
        var tokenResult = identityService.obtainClientCredentials(tokenParameters);
        if (tokenResult.failed()) {
            throw new EdcException(format("Unable to obtain credentials: %s", String.join(", ", tokenResult.getFailureMessages())));
        }
        
        return tokenResult.getContent();
    }
}
