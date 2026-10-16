/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.nats.subscriber;

import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NatsSubscriberTest {

    private final Monitor monitor = mock();
    private final JetStreamSubscription subscription = mock();
    private final List<Message> handled = new CopyOnWriteArrayList<>();
    private TestSubscriber subscriber;

    @AfterEach
    void tearDown() {
        if (subscriber != null) {
            subscriber.stop();
        }
    }

    @Test
    void shouldKeepConsuming_whenFetchFails() {
        var message = mock(Message.class);
        when(subscription.fetch(anyInt(), anyLong()))
                .thenThrow(new IllegalStateException("connection lost"))
                .thenReturn(List.of(message))
                .thenReturn(List.of());
        subscriber = subscriber(m -> StatusResult.success());

        subscriber.startConsuming(subscription);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(handled).containsExactly(message);
            verify(message).ack();
        });
        assertThat(subscriber.isHealthy()).isTrue();
        verify(monitor).severe(anyString(), any(IllegalStateException.class));
    }

    @Test
    void shouldKeepConsuming_whenNakFails() {
        var failing = mock(Message.class);
        doThrow(new IllegalStateException("connection lost")).when(failing).nak();
        var next = mock(Message.class);
        when(subscription.fetch(anyInt(), anyLong()))
                .thenReturn(List.of(failing))
                .thenReturn(List.of(next))
                .thenReturn(List.of());
        subscriber = subscriber(m -> m == failing ? StatusResult.failure(ResponseStatus.ERROR_RETRY, "retry") : StatusResult.success());

        subscriber.startConsuming(subscription);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verify(next).ack());
        assertThat(subscriber.isHealthy()).isTrue();
    }

    @Test
    void shouldReportUnhealthy_whenLoopDiesOnError() {
        var message = mock(Message.class);
        when(subscription.fetch(anyInt(), anyLong())).thenReturn(List.of(message));
        subscriber = subscriber(m -> {
            throw new OutOfMemoryError("simulated");
        });

        subscriber.startConsuming(subscription);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(subscriber.isHealthy()).isFalse());
        verify(monitor).severe(anyString(), any(OutOfMemoryError.class));
    }

    @Test
    void shouldStopQuietly_whenFetchFailsAfterStop() {
        when(subscription.fetch(anyInt(), anyLong())).thenAnswer(i -> {
            Thread.sleep(100);
            throw new IllegalStateException("connection closed");
        });
        subscriber = subscriber(m -> StatusResult.success());

        subscriber.startConsuming(subscription);
        subscriber.stop();

        await().pollDelay(Duration.ofMillis(300)).untilAsserted(() -> verify(monitor, never()).severe(anyString(), any()));
    }

    @Test
    void shouldBeHealthy_whenNotStarted() {
        subscriber = subscriber(m -> StatusResult.success());

        assertThat(subscriber.isHealthy()).isTrue();
        verify(monitor, never()).severe(anyString(), any());
    }

    private TestSubscriber subscriber(Function<Message, StatusResult<Void>> handler) {
        return TestSubscriber.Builder.newInstance(handler, handled)
                .url("nats://localhost:4222")
                .stream("stream")
                .name("test")
                .subject("test.>")
                .monitor(monitor)
                .errorBackoffMillis(10)
                .build();
    }

    private static class TestSubscriber extends NatsSubscriber {

        private Function<Message, StatusResult<Void>> handler;
        private List<Message> handled;

        @Override
        protected StatusResult<Void> handleMessage(Message message) {
            var result = handler.apply(message);
            handled.add(message);
            return result;
        }

        static class Builder extends NatsSubscriber.Builder<TestSubscriber, Builder> {

            private Builder(TestSubscriber subscriber) {
                super(subscriber);
            }

            static Builder newInstance(Function<Message, StatusResult<Void>> handler, List<Message> handled) {
                var subscriber = new TestSubscriber();
                subscriber.handler = handler;
                subscriber.handled = handled;
                return new Builder(subscriber);
            }

            @Override
            public Builder self() {
                return this;
            }
        }
    }
}
