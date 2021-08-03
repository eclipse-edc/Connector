/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.transfer.core.protocol.provider;

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

/**
 *
 */
public class RemoteMessageDispatcherRegistryImpl implements RemoteMessageDispatcherRegistry {
    private Map<String, RemoteMessageDispatcher> dispatchers = new HashMap<>();

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

    @Override
    public void register(RemoteMessageDispatcher dispatcher) {
        dispatchers.put(dispatcher.protocol(), dispatcher);
    }

    @Nullable
    private RemoteMessageDispatcher getDispatcher(@Nullable String protocol) {
        if (protocol == null) {
            if (dispatchers.isEmpty()) {
                return null;
            }
            return dispatchers.values().iterator().next();
        }
        return dispatchers.get(protocol);
    }
}
