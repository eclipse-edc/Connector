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

package org.eclipse.dataspaceconnector.core.event;

import org.eclipse.dataspaceconnector.spi.event.Event;
import org.eclipse.dataspaceconnector.spi.event.EventPayload;
import org.eclipse.dataspaceconnector.spi.event.EventSubscriber;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EventRouterImplTest {

    private final Clock clock = Clock.systemUTC();
    private final Monitor monitor = mock(Monitor.class);
    private final EventRouterImpl eventRouter = new EventRouterImpl(monitor);

    @Test
    void shouldPublishToAllSubscribers() {
        var subscriberA = mock(EventSubscriber.class);
        var subscriberB = mock(EventSubscriber.class);
        eventRouter.register(subscriberA);
        eventRouter.register(subscriberB);

        eventRouter.publish(TestEvent.Builder.newInstance().at(clock.millis()).build());

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(subscriberA).on(isA(TestEvent.class));
            verify(subscriberB).on(isA(TestEvent.class));
        });
    }

    @Test
    void shouldNotInterruptPublishingWhenSubscriberThrowsException() {
        var subscriberA = mock(EventSubscriber.class);
        var subscriberB = mock(EventSubscriber.class);
        doThrow(new RuntimeException("unexpected exception")).when(subscriberA).on(any());
        eventRouter.register(subscriberA);
        eventRouter.register(subscriberB);

        eventRouter.publish(TestEvent.Builder.newInstance().at(clock.millis()).build());

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(subscriberA).on(isA(TestEvent.class));
            verify(subscriberB).on(isA(TestEvent.class));
        });
    }

    private static class TestEvent extends Event<TestEvent.Payload> {
        public static class Builder extends Event.Builder<TestEvent, Payload> {

            public static Builder newInstance() {
                return new Builder();
            }

            private Builder() {
                super(new TestEvent(), new Payload());
            }

            @Override
            protected void validate() {

            }
        }

        public static class Payload extends EventPayload {

        }
    }
}