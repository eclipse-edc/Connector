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
 *
 */

package org.eclipse.dataspaceconnector.spi.message;

import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Binds and sends remote messages using a {@link RemoteMessageDispatcher}.
 * The registry may support multiple protocols and communication patterns, for example HTTP-based and classic message-oriented variants. Consequently, some protocols may be
 * non-blocking, others my be synchronous request-response.
 */
public interface RemoteMessageDispatcherRegistry {

    /**
     * Registers a dispatcher.
     */
    void register(RemoteMessageDispatcher dispatcher);

    /**
     * Sends the message using the given protocol.
     *
     * @param responseType the expected response type
     * @param message      the message
     * @param context      the message context
     * @return a future that can be used to retrieve the response when the operation has completed
     */
    <T> CompletableFuture<T> send(Class<T> responseType, RemoteMessage message, MessageContext context);

}
