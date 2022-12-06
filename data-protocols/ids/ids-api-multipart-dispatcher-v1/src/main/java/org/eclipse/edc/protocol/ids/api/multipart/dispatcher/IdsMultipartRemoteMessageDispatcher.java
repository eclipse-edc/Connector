/*
 *  Copyright (c) 2020 - 2022 Fraunhofer Institute for Software and Systems Engineering
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

package org.eclipse.edc.protocol.ids.api.multipart.dispatcher;

import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.IdsMultipartSender;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.MultipartSenderDelegate;
import org.eclipse.edc.protocol.ids.spi.types.MessageProtocol;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.message.MessageContext;
import org.eclipse.edc.spi.message.RemoteMessageDispatcher;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Dispatcher for IDS Multipart messages.
 */
public class IdsMultipartRemoteMessageDispatcher implements RemoteMessageDispatcher {

    private final IdsMultipartSender multipartSender;
    private final Map<Class<? extends RemoteMessage>, MultipartSenderDelegate<? extends RemoteMessage, ?>> delegates = new HashMap<>();

    public IdsMultipartRemoteMessageDispatcher(IdsMultipartSender idsMultipartSender) {
        this.multipartSender = idsMultipartSender;
    }

    public <M extends RemoteMessage, R> void register(MultipartSenderDelegate<M, R> delegate) {
        delegates.put(delegate.getMessageType(), delegate);
    }

    @Override
    public String protocol() {
        return MessageProtocol.IDS_MULTIPART;
    }

    /**
     * Sends an IDS Multipart message by choosing the correct delegate for the given message type
     * and passing it to the IdsMultipartSender.
     *
     * @param responseType the expected response type
     * @param message the message
     * @param context the message context
     * @return a future that can be used to retrieve the response when the operation has completed
     */
    @Override
    public <T, M extends RemoteMessage> CompletableFuture<T> send(Class<T> responseType, M message, MessageContext context) {
        Objects.requireNonNull(message, "Message was null");
        var delegate = (MultipartSenderDelegate<M, T>) delegates.get(message.getClass());
        if (delegate == null) {
            throw new EdcException("Message sender not found for message type: " + message.getClass().getName());
        }

        return multipartSender.send(message, delegate);
    }

}
