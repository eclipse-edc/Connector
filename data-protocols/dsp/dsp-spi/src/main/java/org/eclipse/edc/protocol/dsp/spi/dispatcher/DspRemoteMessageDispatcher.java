/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.spi.dispatcher;

import org.eclipse.edc.spi.message.RemoteMessageDispatcher;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

/**
 * {@link RemoteMessageDispatcher} for sending dataspace protocol messages.
 */
public interface DspRemoteMessageDispatcher extends RemoteMessageDispatcher {
    
    /**
     * Registers a {@link DspDispatcherDelegate} for supporting a specific type of remote message.
     *
     * @param delegate the delegate
     * @param <M> the type of message
     * @param <R> the response type
     */
    <M extends RemoteMessage, R> void registerDelegate(DspDispatcherDelegate<M, R> delegate);
    
}
