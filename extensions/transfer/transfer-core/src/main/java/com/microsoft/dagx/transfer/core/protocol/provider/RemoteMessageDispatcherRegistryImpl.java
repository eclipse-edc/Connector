package com.microsoft.dagx.transfer.core.protocol.provider;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.message.RemoteMessageDispatcher;
import com.microsoft.dagx.spi.message.RemoteMessageDispatcherRegistry;
import com.microsoft.dagx.spi.types.domain.message.RemoteMessage;
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
    public <T> CompletableFuture<T> send(Class<T> responseType, RemoteMessage message) {
        Objects.requireNonNull(message, "Message was null");
        String protocol = message.getProtocol();
        RemoteMessageDispatcher dispatcher = getDispatcher(protocol);
        if (dispatcher == null) {
            var future = new CompletableFuture<T>();
            future.completeExceptionally(new DagxException("No provider dispatcher registered for protocol: " + protocol));
            return future;
        }
        return dispatcher.send(responseType, message);
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
