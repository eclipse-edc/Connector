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

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Binds and sends remote messages using a {@link RemoteMessageDispatcher}.
 * The registry may support multiple protocols and communication patterns, for example HTTP-based and classic message-oriented variants. Consequently, some protocols may be
 * non-blocking, others may be synchronous request-response.
 */
@ExtensionPoint
public interface RemoteMessageDispatcherRegistry {

    /**
     * Registers a dispatcher.
     */
    void register(String protocol, RemoteMessageDispatcher dispatcher);

    /**
     * Sends the message.
     *
     * @param responseType the expected response type
     * @param message      the message
     * @return a future that can be used to retrieve the response when the operation has completed, it contains a {@link StatusResult}
     */
    <T> CompletableFuture<StatusResult<T>> dispatch(Class<T> responseType, RemoteMessage message);

}
