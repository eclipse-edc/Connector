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

package org.eclipse.dataspaceconnector.spi.event;

/**
 * Central component of the eventing system, the implementation keeps a list of subscribers and notifies them with
 * every events that gets published
 */
public interface EventRouter {

    /**
     * Register a new subscriber to the events
     *
     * @param subscriber that will receive every published event
     */
    void register(EventSubscriber subscriber);

    /**
     * Publish an event to all the subscribers
     *
     * @param event the event to be published
     */
    void publish(Event event);
}
