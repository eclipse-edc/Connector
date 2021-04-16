package com.microsoft.dagx.spi.message;

import com.microsoft.dagx.spi.types.domain.message.RemoteMessage;

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
     * @return a future that can be used to retrieve the response when the operation has completed
     */
    <T> CompletableFuture<T> send(Class<T> responseType, RemoteMessage message);

}
