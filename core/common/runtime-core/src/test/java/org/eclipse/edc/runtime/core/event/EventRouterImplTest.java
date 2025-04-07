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

package org.eclipse.edc.runtime.core.event;

import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        eventRouter.registerSync(TestEvent.class, syncSubscriber);
        eventRouter.register(TestEvent.class, subscriberA);
        eventRouter.register(TestEvent.class, subscriberB);

        var event = EventEnvelope.Builder.newInstance()
                .at(clock.millis())
                .payload(TestEvent.Builder.newInstance().build())
                .build();

        eventRouter.publish(event);

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(syncSubscriber).on(eq(event));
            verify(subscriberA).on(eq(event));
            verify(subscriberB).on(eq(event));
        });
    }

    @Test
    void shouldPublishToSyncSubscribers() {
        var syncSubscriber = mock(EventSubscriber.class);
        var syncSubscriberAll = mock(EventSubscriber.class);
        var syncSubscriberBase = mock(EventSubscriber.class);

        eventRouter.registerSync(TestEvent.class, syncSubscriber);
        eventRouter.registerSync(Event.class, syncSubscriberAll);
        eventRouter.registerSync(TestEventBase.class, syncSubscriberBase);

        var event = EventEnvelope.Builder.newInstance()
                .at(clock.millis())
                .payload(TestEvent.Builder.newInstance().build())
                .build();

        eventRouter.publish(event);

        verify(syncSubscriber).on(eq(event));
        verify(syncSubscriberAll).on(eq(event));
        verify(syncSubscriberBase).on(eq(event));
    }

    @Test
    void shouldPublishToAsyncSubscribers() {
        var subscriber = mock(EventSubscriber.class);
        var subscriberAll = mock(EventSubscriber.class);
        var subscriberBase = mock(EventSubscriber.class);

        eventRouter.register(TestEvent.class, subscriber);
        eventRouter.register(Event.class, subscriberAll);
        eventRouter.register(TestEventBase.class, subscriberBase);

        var event = EventEnvelope.Builder.newInstance()
                .at(clock.millis())
                .payload(TestEvent.Builder.newInstance().build())
                .build();

        eventRouter.publish(event);

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(subscriber).on(eq(event));
            verify(subscriberAll).on(eq(event));
            verify(subscriberBase).on(eq(event));
        });
    }


    @Test
    void shouldNotInterruptPublishingWhenSubscriberThrowsException() {
        var subscriberA = mock(EventSubscriber.class);
        var subscriberB = mock(EventSubscriber.class);
        doThrow(new RuntimeException("unexpected exception")).when(subscriberA).on(any());
        eventRouter.register(TestEvent.class, subscriberA);
        eventRouter.register(TestEvent.class, subscriberB);

        var event = EventEnvelope.Builder.newInstance()
                .at(clock.millis())
                .payload(TestEvent.Builder.newInstance().build())
                .build();
        eventRouter.publish(event);

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(subscriberA).on(eq(event));
            verify(subscriberB).on(eq(event));
        });
    }

    @Test
    void shouldInterruptPublishingWhenSyncSubscriberThrowsException() {
        var subscriberA = mock(EventSubscriber.class);
        var subscriberB = mock(EventSubscriber.class);
        doThrow(new RuntimeException("unexpected exception")).when(subscriberA).on(any());
        eventRouter.registerSync(TestEvent.class, subscriberA);
        eventRouter.register(TestEvent.class, subscriberB);

        var event = EventEnvelope.Builder.newInstance()
                .at(clock.millis())
                .payload(TestEvent.Builder.newInstance().build())
                .build();

        assertThatThrownBy(() -> eventRouter.publish(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("unexpected exception");
        verifyNoInteractions(subscriberB);
    }

    private abstract static class TestEventBase extends Event {
    }

    private static class TestEvent extends TestEventBase {

        @Override
        public String name() {
            return "test";
        }

        public static class Builder {

            private final TestEvent event;

            private Builder() {
                event = new TestEvent();
            }

            public static TestEvent.Builder newInstance() {
                return new TestEvent.Builder();
            }

            public TestEvent build() {
                return event;
            }
        }
    }
}
