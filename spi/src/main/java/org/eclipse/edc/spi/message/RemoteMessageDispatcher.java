/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.spi.message;

import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Dispatches remote messages to a destination.
 */
public interface RemoteMessageDispatcher {

    /**
     * Return the protocol this dispatcher uses.
     */
    String protocol();

    /**
     * Binds and sends the message.
     *
     * @param responseType the expected response type
     * @param message the message
     * @param context the message context
     * @return a future that can be used to retrieve the response when the operation has completed
     */
    <T> CompletableFuture<T> send(Class<T> responseType, RemoteMessage message, MessageContext context);

}
