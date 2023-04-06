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

package org.eclipse.edc.connector.callback.dispatcher.http;

import org.eclipse.edc.spi.message.RemoteMessageDispatcher;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Wrapper for {@link GenericHttpRemoteDispatcher} for handling both http/https protocol
 */
public class GenericHttpRemoteDispatcherWrapper implements RemoteMessageDispatcher {
    private final GenericHttpRemoteDispatcher delegate;
    private final String protocol;

    public GenericHttpRemoteDispatcherWrapper(GenericHttpRemoteDispatcher delegate, String protocol) {
        this.delegate = delegate;
        this.protocol = protocol;
    }

    @Override
    public String protocol() {
        return protocol;
    }

    @Override
    public <T, M extends RemoteMessage> CompletableFuture<T> send(Class<T> responseType, M message) {
        return delegate.send(responseType, message);
    }

}
