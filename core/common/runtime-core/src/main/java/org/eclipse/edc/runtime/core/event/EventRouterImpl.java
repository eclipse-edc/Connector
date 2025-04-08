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

package org.eclipse.edc.runtime.core.event;

import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.runAsync;

public class EventRouterImpl implements EventRouter {

    private final Map<Class<?>, List<EventSubscriber>> subscribers = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<EventSubscriber>> syncSubscribers = new ConcurrentHashMap<>();

    private final Monitor monitor;
    private final ExecutorService executor;

    public EventRouterImpl(Monitor monitor, ExecutorService executor) {
        this.monitor = monitor;
        this.executor = executor;
    }

    @Override
    public <E extends Event> void registerSync(Class<E> eventKind, EventSubscriber subscriber) {
        syncSubscribers.computeIfAbsent(eventKind, s -> new ArrayList<>()).add(subscriber);
    }

    @Override
    public <E extends Event> void register(Class<E> eventKind, EventSubscriber subscriber) {
        subscribers.computeIfAbsent(eventKind, s -> new ArrayList<>()).add(subscriber);
    }
    
    @Override
    public <E extends Event> void publish(EventEnvelope<E> event) {
        subscriberFor(event, this::getSyncSubscribers).forEach(subscriber -> subscriber.on(event));

        subscriberFor(event, this::getSubscribers)
                .map(subscriber -> runAsync(() -> subscriber.on(event), executor).thenApply(v -> subscriber))
                .forEach(future -> future.whenComplete((subscriber, throwable) -> {
                    if (throwable != null) {
                        var subscriberName = subscriber.getClass().getSimpleName();
                        var eventName = event.getClass().getSimpleName();
                        monitor.severe(format("Subscriber %s failed to handle event %s", subscriberName, eventName), throwable);
                    }
                }));
    }

    private Map<Class<?>, List<EventSubscriber>> getSubscribers() {
        return subscribers;
    }

    private Map<Class<?>, List<EventSubscriber>> getSyncSubscribers() {
        return syncSubscribers;
    }

    private <E extends Event> Stream<EventSubscriber> subscriberFor(EventEnvelope<E> envelope, Supplier<Map<Class<?>, List<EventSubscriber>>> supplier) {
        return supplier.get().entrySet()
                .stream()
                .filter(entry -> entry.getKey().isAssignableFrom(envelope.getPayload().getClass()))
                .flatMap(entry -> entry.getValue().stream());
    }
}
