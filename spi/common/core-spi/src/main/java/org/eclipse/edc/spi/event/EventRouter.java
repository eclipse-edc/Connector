/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.spi.event;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Central component of the eventing system, the implementation keeps a list of subscribers and notifies them with
 * every events that gets published
 */
@ExtensionPoint
public interface EventRouter {

    /**
     * Register a new synchronous subscriber.
     * The synchronous subscribers are supposed to being notified before the standard subscribers in a synchronous way.
     * If a synchronous subscriber thrown an exception, this will stop the publish operation.
     *
     * @param subscriber that will receive every published event
     */
    <E extends Event> void registerSync(Class<E> eventKind, EventSubscriber subscriber);

    /**
     * Register a new asynchronous subscriber to the events
     *
     * @param subscriber that will receive every published event
     */
    <E extends Event> void register(Class<E> eventKind, EventSubscriber subscriber);

    /**
     * Publish an event to all the subscribers
     *
     * @param event the event to be published
     */
    <E extends Event> void publish(E event);

    /**
     * Publish an eventEnvelope to all the subscribers
     *
     * @param eventEnvelope the eventEnvelope to be published
     */
    <E extends Event> void publish(EventEnvelope<E> eventEnvelope);
}
