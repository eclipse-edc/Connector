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

package org.eclipse.edc.connector.callback.dispatcher;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.callback.CallbackEventRemoteMessage;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Subscriber for invoking callbacks associated to {@link Event}. If the {@link CallbackAddress#getEvents()} matches
 * the {@link Event#name()}, the callback is the invoked using a {@link RemoteMessageDispatcherRegistry} with protocol
 * extracted by {@link CallbackAddress#getUri()}
 */
public class CallbackEventDispatcher<T extends Event> implements EventSubscriber<T> {
    private final RemoteMessageDispatcherRegistry dispatcher;
    private final boolean transactional;
    private final Monitor monitor;

    public CallbackEventDispatcher(RemoteMessageDispatcherRegistry dispatcher, boolean transactional, Monitor monitor) {
        this.dispatcher = dispatcher;
        this.transactional = transactional;
        this.monitor = monitor;
    }

    @Override
    public void on(EventEnvelope<T> eventEnvelope) {
        var callbacks = eventEnvelope.getPayload().getCallbackAddresses()
                .stream().filter(cb -> cb.isTransactional() == transactional)
                .collect(Collectors.toList());

        var eventName = eventEnvelope.getPayload().name();

        for (var callback : callbacks) {
            if (matches(eventName, callback)) {
                try {
                    dispatcher.send(Object.class, new CallbackEventRemoteMessage<>(callback, eventEnvelope)).get();
                } catch (Exception e) {
                    monitor.severe(format("Failed to invoke callback at URI: %s", callback.getUri()), e);
                    throw new EdcException(e);
                }
            }
        }
    }

    public boolean isTransactional() {
        return transactional;
    }

    private boolean matches(String eventName, CallbackAddress callbackAddress) {
        return callbackAddress.getEvents().stream().anyMatch(eventName::startsWith);
    }
}
