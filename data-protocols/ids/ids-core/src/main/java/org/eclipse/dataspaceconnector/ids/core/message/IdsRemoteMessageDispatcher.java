/*
 *  Copyright (c) 2020, 2021 Fraunhofer Institute for Software and Systems Engineering
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

package org.eclipse.dataspaceconnector.ids.core.message;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcher;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract class for IDS message dispatchers, as the IDS dispatchers for protocols multipart and
 * rest differ only in their implementation of the protocol() method.
 */
public abstract class IdsRemoteMessageDispatcher implements RemoteMessageDispatcher {

    private final Map<Class<? extends RemoteMessage>, IdsMessageSender<? extends RemoteMessage, ?>> senders = new HashMap<>();

    public void register(IdsMessageSender<? extends RemoteMessage, ?> handler) {
        senders.put(handler.messageType(), handler);
    }

    @Override
    public <T> CompletableFuture<T> send(Class<T> responseType, RemoteMessage message, MessageContext context) {
        Objects.requireNonNull(message, "Message was null");
        IdsMessageSender<RemoteMessage, ?> handler = (IdsMessageSender<RemoteMessage, ?>) senders.get(message.getClass());
        if (handler == null) {
            throw new EdcException("Message sender not found for message type: " + message.getClass().getName());
        }
        return (CompletableFuture<T>) handler.send(message);
    }

}
