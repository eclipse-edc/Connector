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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - refactor to base module
 *
 */

package org.eclipse.edc.runtime.core.message;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.message.RemoteMessageDispatcher;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.failedFuture;

public class RemoteMessageDispatcherRegistryImpl implements RemoteMessageDispatcherRegistry {

    private final Map<String, RemoteMessageDispatcher> dispatchers = new HashMap<>();

    @Override
    public void register(String protocol, RemoteMessageDispatcher dispatcher) {
        dispatchers.put(protocol, dispatcher);
    }

    @Override
    public <T> CompletableFuture<StatusResult<T>> dispatch(String participantContextId, Class<T> responseType, RemoteMessage message) {
        Objects.requireNonNull(message, "Message was null");
        var protocol = message.getProtocol();
        var dispatcher = dispatchers.get(protocol);
        if (dispatcher == null) {
            return failedFuture(new EdcException("No provider dispatcher registered for protocol: " + protocol));
        }
        return dispatcher.dispatch(participantContextId, responseType, message);
    }

}
