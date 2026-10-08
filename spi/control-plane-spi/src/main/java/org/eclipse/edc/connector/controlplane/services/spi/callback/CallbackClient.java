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

package org.eclipse.edc.connector.controlplane.services.spi.callback;

import org.eclipse.edc.controlplane.CallbackAddress;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.jetbrains.annotations.Nullable;

/**
 * Client for dispatching events to callback endpoints.
 */
public interface CallbackClient {

    <E extends Event> void dispatch(CallbackAddress callbackAddress, EventEnvelope<E> eventEnvelope);

    /**
     * Dispatches the event to the callback, resolving the callback secrets from the given vault partition.
     *
     * @param callbackAddress the callback address.
     * @param eventEnvelope the event envelope.
     * @param vaultPartition the vault partition used to resolve the callback secrets, null for the default partition.
     */
    default <E extends Event> void dispatch(CallbackAddress callbackAddress, EventEnvelope<E> eventEnvelope, @Nullable String vaultPartition) {
        dispatch(callbackAddress, eventEnvelope);
    }

}
