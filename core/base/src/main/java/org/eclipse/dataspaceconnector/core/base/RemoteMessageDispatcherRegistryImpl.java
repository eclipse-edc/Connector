/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - refactor to base module
 *
 */

package org.eclipse.dataspaceconnector.core.base;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcher;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class RemoteMessageDispatcherRegistryImpl implements RemoteMessageDispatcherRegistry {

    private final Map<String, RemoteMessageDispatcher> dispatchers = new HashMap<>();

    @Override
    public void register(RemoteMessageDispatcher dispatcher) {
        dispatchers.put(dispatcher.protocol(), dispatcher);
    }

    @Override
    public <T> CompletableFuture<T> send(Class<T> responseType, RemoteMessage message, MessageContext context) {
        Objects.requireNonNull(message, "Message was null");
        String protocol = message.getProtocol();
        RemoteMessageDispatcher dispatcher = getDispatcher(protocol);
        if (dispatcher == null) {
            var future = new CompletableFuture<T>();
            future.completeExceptionally(new EdcException("No provider dispatcher registered for protocol: " + protocol));
            return future;
        }
        return dispatcher.send(responseType, message, context);
    }

    @Nullable
    private RemoteMessageDispatcher getDispatcher(@Nullable String protocol) {
        if (protocol == null) {
            return dispatchers.values().stream().findFirst()
                    .orElse(null);
        }
        return dispatchers.get(protocol);
    }
}
