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
     * @param message      the message
     * @param context      the message context
     * @return a future that can be used to retrieve the response when the operation has completed
     */
    <T, M extends RemoteMessage> CompletableFuture<T> send(Class<T> responseType, M message, MessageContext context);

}
