/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.callback.dispatcher.http;

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

/**
 * Default implementation for {@link GenericHttpRemoteDispatcher}
 */
public class GenericHttpRemoteDispatcherImpl implements GenericHttpRemoteDispatcher {

    public static final String CALLBACK_EVENT_HTTP = "callback-event-http";
    private final EdcHttpClient httpClient;
    private final Map<Class<? extends RemoteMessage>, GenericHttpDispatcherDelegate> delegates = new HashMap<>();

    protected GenericHttpRemoteDispatcherImpl(EdcHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public <T, M extends RemoteMessage> CompletableFuture<StatusResult<T>> dispatch(Class<T> responseType, M message) {
        var delegate = (GenericHttpDispatcherDelegate<M, T>) delegates.get(message.getClass());
        if (delegate == null) {
            throw new EdcException(format("No %s message dispatcher found for message type %s", CALLBACK_EVENT_HTTP, message.getClass()));
        }
        var request = delegate.buildRequest(message);
        return httpClient.executeAsync(request, emptyList())
                .thenApply(response -> {
                    try (response) {
                        return delegate.parseResponse().apply(response);
                    }
                })
                .thenApply(StatusResult::success);
    }

    @Override
    public <M extends RemoteMessage, R> void registerDelegate(GenericHttpDispatcherDelegate<M, R> delegate) {
        this.delegates.put(delegate.getMessageType(), delegate);
    }
}
