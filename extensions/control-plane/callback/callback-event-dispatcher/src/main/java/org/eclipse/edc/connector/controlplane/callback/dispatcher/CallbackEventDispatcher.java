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

import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackClient;
import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackRegistry;
import org.eclipse.edc.connector.controlplane.services.spi.callback.ParticipantCallbackResolver;
import org.eclipse.edc.controlplane.CallbackAddress;
import org.eclipse.edc.controlplane.CallbackAddresses;
import org.eclipse.edc.participantcontext.spi.types.ParticipantEvent;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Subscriber for invoking callbacks associated to {@link Event}. If the {@link CallbackAddress#getEvents()} matches
 * the {@link Event#name()}, the callback is invoked using a {@link CallbackClient}.
 */
public class CallbackEventDispatcher implements EventSubscriber {
    private final CallbackClient callbackClient;
    private final boolean transactional;
    private final Monitor monitor;
    private final CallbackRegistry callbackRegistry;
    private final ParticipantCallbackResolver participantCallbackResolver;

    public CallbackEventDispatcher(CallbackClient callbackClient, CallbackRegistry callbackRegistry, boolean transactional, Monitor monitor) {
        this(callbackClient, callbackRegistry, (participantContextId, eventName) -> List.of(), transactional, monitor);
    }

    public CallbackEventDispatcher(CallbackClient callbackClient, CallbackRegistry callbackRegistry, ParticipantCallbackResolver participantCallbackResolver,
                                   boolean transactional, Monitor monitor) {
        this.callbackClient = callbackClient;
        this.callbackRegistry = callbackRegistry;
        this.participantCallbackResolver = participantCallbackResolver;
        this.transactional = transactional;
        this.monitor = monitor;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> eventEnvelope) {
        var callbacks = getCallbacks(eventEnvelope);
        var eventName = eventEnvelope.getPayload().name();

        for (var callback : callbacks) {
            if (matches(eventName, callback)) {
                invoke(callback, () -> callbackClient.dispatch(callback, eventEnvelope));
            }
        }

        // participant callbacks resolve their secrets from the vault partition of the participant context
        if (eventEnvelope.getPayload() instanceof ParticipantEvent participantEvent && participantEvent.getParticipantContextId() != null) {
            var participantContextId = participantEvent.getParticipantContextId();
            participantCallbackResolver.resolve(participantContextId, eventName).stream()
                    .filter(cb -> cb.isTransactional() == transactional)
                    .filter(cb -> matches(eventName, cb))
                    .forEach(callback -> invoke(callback, () -> callbackClient.dispatch(callback, eventEnvelope, participantContextId)));
        }
    }

    public boolean isTransactional() {
        return transactional;
    }

    private void invoke(CallbackAddress callback, Runnable dispatch) {
        try {
            dispatch.run();
        } catch (Exception e) {
            monitor.severe(format("Failed to invoke callback at URI: %s", callback.getUri()), e);
            throw new EdcException(e);
        }
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
