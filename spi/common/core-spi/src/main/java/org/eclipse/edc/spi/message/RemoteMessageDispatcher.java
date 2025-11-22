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

import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Dispatches remote messages to a destination.
 */
public interface RemoteMessageDispatcher<RM extends RemoteMessage> {


    /**
     * Binds and sends the message.
     *
     * @param responseType the expected response type
     * @param message      the message
     * @return a future that can be used to retrieve the response when the operation has completed
     */
    <T, M extends RM> CompletableFuture<StatusResult<T>> dispatch(String participantContextId, Class<T> responseType, M message);

}
