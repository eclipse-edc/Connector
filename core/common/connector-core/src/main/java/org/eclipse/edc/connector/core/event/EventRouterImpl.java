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
 *       Masatake Iwasaki (NTT DATA) - refactored to use dedicated thread pool
 *
 */

package org.eclipse.edc.connector.core.event;

import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.runAsync;

public class EventRouterImpl implements EventRouter {

    private final List<EventSubscriber> subscribers = new ArrayList<>();
    private final List<EventSubscriber> syncSubscribers = new ArrayList<>();
    private final Monitor monitor;
    private final ExecutorService executor;

    public EventRouterImpl(Monitor monitor, ExecutorService executor) {
        this.monitor = monitor;
        this.executor = executor;
    }

    @Override
    public void registerSync(EventSubscriber subscriber) {
        this.syncSubscribers.add(subscriber);
    }

    @Override
    public void register(EventSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    @Override
    public void publish(Event event) {
        syncSubscribers.forEach(subscriber -> subscriber.on(event));

        subscribers.stream()
                .map(subscriber -> runAsync(() -> subscriber.on(event), executor).thenApply(v -> subscriber))
                .forEach(future -> future.whenComplete((subscriber, throwable) -> {
                    if (throwable != null) {
                        var subscriberName = subscriber.getClass().getSimpleName();
                        var eventName = event.getClass().getSimpleName();
                        monitor.severe(format("Subscriber %s failed to handle event %s", subscriberName, eventName), throwable);
                    }
                }));
    }
}
