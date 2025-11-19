/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.callback.dispatcher.http;

import org.eclipse.edc.spi.message.RemoteMessageDispatcher;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

/**
 * Generic HTTP Remote dispatcher
 */
public interface GenericHttpRemoteDispatcher extends RemoteMessageDispatcher<RemoteMessage> {

    /**
     * Registers a {@link GenericHttpDispatcherDelegate} for supporting a specific type of remote message.
     *
     * @param delegate the delegate
     * @param <M> the type of message
     */
    <M extends RemoteMessage, R> void registerDelegate(GenericHttpDispatcherDelegate<M, R> delegate);
}
