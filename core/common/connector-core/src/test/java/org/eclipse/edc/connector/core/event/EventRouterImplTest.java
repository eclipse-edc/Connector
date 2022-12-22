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

package org.eclipse.edc.connector.core.event;

import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventPayload;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class EventRouterImplTest {

    private final Clock clock = Clock.systemUTC();
    private final Monitor monitor = mock(Monitor.class);
    private final EventRouterImpl eventRouter = new EventRouterImpl(monitor, Executors.newSingleThreadExecutor());

    @Test
    void shouldPublishToAllSubscribers() {
        var syncSubscriber = mock(EventSubscriber.class);
        var subscriberA = mock(EventSubscriber.class);
        var subscriberB = mock(EventSubscriber.class);
        eventRouter.registerSync(syncSubscriber);
        eventRouter.register(subscriberA);
        eventRouter.register(subscriberB);

        eventRouter.publish(TestEvent.Builder.newInstance().at(clock.millis()).build());

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(syncSubscriber).on(isA(TestEvent.class));
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

    @Test
    void shouldInterruptPublishingWhenSyncSubscriberThrowsException() {
        var subscriberA = mock(EventSubscriber.class);
        var subscriberB = mock(EventSubscriber.class);
        doThrow(new RuntimeException("unexpected exception")).when(subscriberA).on(any());
        eventRouter.registerSync(subscriberA);
        eventRouter.register(subscriberB);

        assertThatThrownBy(() -> eventRouter.publish(TestEvent.Builder.newInstance().at(clock.millis()).build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("unexpected exception");
        verifyNoInteractions(subscriberB);
    }

    private static class TestEvent extends Event<TestEvent.Payload> {

        public static class Payload extends EventPayload {

        }

        public static class Builder extends Event.Builder<TestEvent, Payload, Builder> {

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

    }
}
