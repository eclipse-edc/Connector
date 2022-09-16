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

package org.eclipse.dataspaceconnector.ids.core.message;

import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;

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
    CompletableFuture<R> send(M message);

}
