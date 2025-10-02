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

package org.eclipse.edc.connector.controlplane.callback.dispatcher;

import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackEventRemoteMessage;
import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackProtocolResolverRegistry;
import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.CallbackAddresses;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Subscriber for invoking callbacks associated to {@link Event}. If the {@link CallbackAddress#getEvents()} matches
 * the {@link Event#name()}, the callback is the invoked using a {@link RemoteMessageDispatcherRegistry} with protocol
 * extracted by {@link CallbackAddress#getUri()}
 */
public class CallbackEventDispatcher implements EventSubscriber {
    private final RemoteMessageDispatcherRegistry dispatcher;
    private final boolean transactional;
    private final Monitor monitor;
    private final CallbackRegistry callbackRegistry;
    private final CallbackProtocolResolverRegistry resolverRegistry;

    public CallbackEventDispatcher(RemoteMessageDispatcherRegistry dispatcher, CallbackRegistry callbackRegistry, CallbackProtocolResolverRegistry resolveRegistry, boolean transactional, Monitor monitor) {
        this.dispatcher = dispatcher;
        this.callbackRegistry = callbackRegistry;
        this.transactional = transactional;
        this.resolverRegistry = resolveRegistry;
        this.monitor = monitor;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> eventEnvelope) {
        var callbacks = getCallbacks(eventEnvelope);
        var eventName = eventEnvelope.getPayload().name();

        for (var callback : callbacks) {
            if (matches(eventName, callback)) {
                try {
                    var protocol = resolverRegistry.resolve(URI.create(callback.getUri()).getScheme());
                    if (protocol != null) {
                        // TODO refactor events to carry the participant context id
                        dispatcher.dispatch(null, Object.class, new CallbackEventRemoteMessage<>(callback, eventEnvelope, protocol)).get();
                    } else {
                        monitor.warning(format("Failed to resolve protocol for URI %s", callback.getUri()));
                    }
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

    private <E extends Event> List<CallbackAddress> getCallbacks(EventEnvelope<E> eventEnvelope) {
        var staticCallbacks = callbackRegistry.resolve(eventEnvelope.getPayload().name()).stream();

        var dynamicCallbacks = eventEnvelope.getPayload() instanceof CallbackAddresses callbackAddresses ?
                callbackAddresses.getCallbackAddresses().stream() : Stream.<CallbackAddress>empty();

        return Stream.concat(staticCallbacks, dynamicCallbacks)
                .filter(cb -> cb.isTransactional() == transactional)
                .collect(Collectors.toList());
    }

    private boolean matches(String eventName, CallbackAddress callbackAddress) {
        return callbackAddress.getEvents().stream().anyMatch(eventName::startsWith);
    }
}
