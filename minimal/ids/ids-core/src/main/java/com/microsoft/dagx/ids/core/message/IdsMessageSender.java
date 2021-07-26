/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.core.message;

import com.microsoft.dagx.spi.message.MessageContext;
import com.microsoft.dagx.spi.types.domain.message.RemoteMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Binds and sends a remote message type using the IDS REST protocol.
 */
public interface IdsMessageSender<M extends RemoteMessage, R> {

    /**
     * Returns the IDS message type this handler supports.
     */
    Class<M> messageType();

    /**
     * Binds and sends the message, returning a future for retrieving the response.
     */
    CompletableFuture<R> send(M message, MessageContext context);

}
