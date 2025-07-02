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
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class EventRouterImplTest {

    private final Instant instant = Instant.now();
    private final Clock clock = Clock.fixed(instant, ZoneId.systemDefault());
    private final Monitor monitor = mock(Monitor.class);
    private final EventRouterImpl eventRouter = new EventRouterImpl(monitor, Executors.newSingleThreadExecutor(), clock);

    @Test
    void shouldPublishToAllSubscribers() {
        var syncSubscriber = mock(EventSubscriber.class);
        var subscriberA = mock(EventSubscriber.class);
        var subscriberB = mock(EventSubscriber.class);

        eventRouter.registerSync(TestEvent.class, syncSubscriber);
        eventRouter.register(TestEvent.class, subscriberA);
        eventRouter.register(TestEvent.class, subscriberB);

        var event = TestEvent.Builder.newInstance().build();

        eventRouter.publish(event);

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            ArgumentCaptor<EventEnvelope<Event>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
            verify(syncSubscriber).on(captor.capture());
            verify(subscriberA).on(captor.capture());
            verify(subscriberB).on(captor.capture());
            assertThat(captor.getAllValues()).hasSize(3).allSatisfy(envelope -> {
                assertThat(envelope.getPayload()).isSameAs(event);
                assertThat(envelope.getAt()).isEqualTo(instant.toEpochMilli());
            });
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

        var event = TestEvent.Builder.newInstance().build();

        eventRouter.publish(event);

        ArgumentCaptor<EventEnvelope<Event>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(syncSubscriber).on(captor.capture());
        verify(syncSubscriberAll).on(captor.capture());
        verify(syncSubscriberBase).on(captor.capture());
        assertThat(captor.getAllValues()).hasSize(3).allSatisfy(envelope -> {
            assertThat(envelope.getPayload()).isSameAs(event);
            assertThat(envelope.getAt()).isEqualTo(instant.toEpochMilli());
        });
    }

    @Test
    void shouldPublishToAsyncSubscribers() {
        var subscriber = mock(EventSubscriber.class);
        var subscriberAll = mock(EventSubscriber.class);
        var subscriberBase = mock(EventSubscriber.class);

        eventRouter.register(TestEvent.class, subscriber);
        eventRouter.register(Event.class, subscriberAll);
        eventRouter.register(TestEventBase.class, subscriberBase);

        var event = TestEvent.Builder.newInstance().build();

        eventRouter.publish(event);

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            ArgumentCaptor<EventEnvelope<Event>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
            verify(subscriber).on(captor.capture());
            verify(subscriberAll).on(captor.capture());
            verify(subscriberBase).on(captor.capture());
            assertThat(captor.getAllValues()).hasSize(3).allSatisfy(envelope -> {
                assertThat(envelope.getPayload()).isSameAs(event);
                assertThat(envelope.getAt()).isEqualTo(instant.toEpochMilli());
            });
        });
    }

    @Test
    void shouldNotInterruptPublishingWhenSubscriberThrowsException() {
        var subscriberA = mock(EventSubscriber.class);
        var subscriberB = mock(EventSubscriber.class);
        doThrow(new RuntimeException("unexpected exception")).when(subscriberA).on(any());
        eventRouter.register(TestEvent.class, subscriberA);
        eventRouter.register(TestEvent.class, subscriberB);

        var event = TestEvent.Builder.newInstance().build();

        eventRouter.publish(event);

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            ArgumentCaptor<EventEnvelope<Event>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
            verify(subscriberA).on(captor.capture());
            verify(subscriberB).on(captor.capture());
            assertThat(captor.getAllValues()).hasSize(2).allSatisfy(envelope -> {
                assertThat(envelope.getPayload()).isSameAs(event);
                assertThat(envelope.getAt()).isEqualTo(instant.toEpochMilli());
            });
        });
    }

    @Test
    void shouldInterruptPublishingWhenSyncSubscriberThrowsException() {
        var subscriberA = mock(EventSubscriber.class);
        var subscriberB = mock(EventSubscriber.class);
        doThrow(new RuntimeException("unexpected exception")).when(subscriberA).on(any());
        eventRouter.registerSync(TestEvent.class, subscriberA);
        eventRouter.register(TestEvent.class, subscriberB);

        var event = TestEvent.Builder.newInstance().build();

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
