/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.ids.core.message;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.message.MessageContext;
import org.eclipse.edc.spi.message.RemoteMessageDispatcher;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.edc.ids.spi.Protocols.IDS_REST;

/**
 * Binds and sends remote messages using the IDS REST protocol by dispatching to {@link IdsMessageSender}s.
 */
public class IdsRemoteMessageDispatcher implements RemoteMessageDispatcher {
    private Map<Class<? extends RemoteMessage>, IdsMessageSender<? extends RemoteMessage, ?>> senders = new HashMap<>();

    public void register(IdsMessageSender<? extends RemoteMessage, ?> handler) {
        senders.put(handler.messageType(), handler);
    }

    @Override
    public String protocol() {
        return IDS_REST;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> send(Class<T> responseType, RemoteMessage message, MessageContext context) {
        Objects.requireNonNull(message, "Message was null");
        IdsMessageSender<RemoteMessage, ?> handler = (IdsMessageSender<RemoteMessage, ?>) senders.get(message.getClass());
        if (handler == null) {
            throw new EdcException("Message sender not found for message type: " + message.getClass().getName());
        }
        return (CompletableFuture<T>) handler.send(message, context);
    }

}
