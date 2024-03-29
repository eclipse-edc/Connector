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

package org.eclipse.edc.connector.controlplane.spi.callback;

import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

/**
 * Envelope for implementors {@link Event} for sending event to external systems via {@link RemoteMessageDispatcherRegistry}
 */
public class CallbackEventRemoteMessage<T extends Event> implements RemoteMessage {

    private final String protocol;
    private final EventEnvelope<T> envelope;
    private final CallbackAddress callbackAddress;

    public CallbackEventRemoteMessage(CallbackAddress callbackAddress, EventEnvelope<T> envelope, String protocol) {
        this.callbackAddress = callbackAddress;
        this.protocol = protocol;
        this.envelope = envelope;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getCounterPartyAddress() {
        return callbackAddress.getUri();
    }

    public String getAuthKey() {
        return callbackAddress.getAuthKey();
    }

    public String getAuthCodeId() {
        return callbackAddress.getAuthCodeId();
    }

    public EventEnvelope<T> getEventEnvelope() {
        return envelope;
    }

    @Override
    public String getCounterPartyId() {
        return null;
    }
}
