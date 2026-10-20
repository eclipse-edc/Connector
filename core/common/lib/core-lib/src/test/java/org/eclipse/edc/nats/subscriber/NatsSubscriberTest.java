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
import io.nats.client.impl.NatsJetStreamMetaData;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NatsSubscriberTest {

    private static final long RETRY_BASE_DELAY = 100;

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
        var failing = messageDeliveredTimes(1);
        doThrow(new IllegalStateException("connection lost")).when(failing).nakWithDelay(any(Duration.class));
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
    void shouldNakWithExponentialDelay_whenResultIsRetryable() {
        var first = messageDeliveredTimes(1);
        var third = messageDeliveredTimes(3);
        when(subscription.fetch(anyInt(), anyLong()))
                .thenReturn(List.of(first, third))
                .thenReturn(List.of());
        subscriber = subscriber(m -> StatusResult.failure(ResponseStatus.ERROR_RETRY, "retry"));

        subscriber.startConsuming(subscription);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(first).nakWithDelay(Duration.ofMillis(RETRY_BASE_DELAY * 2));
            verify(third).nakWithDelay(Duration.ofMillis(RETRY_BASE_DELAY * 8));
        });
        verify(first, never()).nak();
    }

    @Test
    void shouldCapRetryDelay_whenDeliveredManyTimes() {
        var message = messageDeliveredTimes(1000);
        when(subscription.fetch(anyInt(), anyLong()))
                .thenReturn(List.of(message))
                .thenReturn(List.of());
        subscriber = subscriber(m -> StatusResult.failure(ResponseStatus.ERROR_RETRY, "retry"));

        subscriber.startConsuming(subscription);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                verify(message).nakWithDelay(Duration.ofMillis(RETRY_BASE_DELAY * (1L << NatsSubscriber.MAX_RETRY_DELAY_EXPONENT))));
    }

    @Test
    void shouldTerm_whenResultIsFatal() {
        var message = messageDeliveredTimes(1);
        when(subscription.fetch(anyInt(), anyLong()))
                .thenReturn(List.of(message))
                .thenReturn(List.of());
        subscriber = subscriber(m -> StatusResult.failure(ResponseStatus.FATAL_ERROR, "fatal"));

        subscriber.startConsuming(subscription);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verify(message).term());
        verify(message, never()).nakWithDelay(any(Duration.class));
    }

    private Message messageDeliveredTimes(long deliveredCount) {
        var metaData = mock(NatsJetStreamMetaData.class);
        when(metaData.deliveredCount()).thenReturn(deliveredCount);
        var message = mock(Message.class);
        when(message.metaData()).thenReturn(metaData);
        return message;
    }

    @Test
    void shouldMarkHeldMessagesInProgress_whileProcessing() {
        var slow = messageDeliveredTimes(1);
        var waiting = messageDeliveredTimes(1);
        when(subscription.fetch(anyInt(), anyLong()))
                .thenReturn(List.of(slow, waiting))
                .thenReturn(List.of());
        subscriber = subscriber(m -> {
            if (m == slow) {
                sleep(300);
            }
            return StatusResult.success();
        });

        subscriber.startConsuming(subscription);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verify(waiting).ack());
        // the message being processed and the one waiting in the batch are both kept alive
        verify(slow, atLeast(2)).inProgress();
        verify(waiting, atLeast(2)).inProgress();
    }

    @Test
    void shouldNotMarkInProgress_whenMessageIsAcknowledged() {
        var message = messageDeliveredTimes(1);
        when(subscription.fetch(anyInt(), anyLong()))
                .thenReturn(List.of(message))
                .thenReturn(List.of());
        subscriber = subscriber(m -> StatusResult.success());

        subscriber.startConsuming(subscription);
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verify(message).ack());
        var invocations = mockingDetails(message).getInvocations().size();

        await().pollDelay(Duration.ofMillis(300)).untilAsserted(() ->
                assertThat(mockingDetails(message).getInvocations()).hasSize(invocations));
    }

    @Test
    void shouldKeepProcessing_whenMarkInProgressFails() {
        var message = messageDeliveredTimes(1);
        doThrow(new IllegalStateException("connection lost")).when(message).inProgress();
        when(subscription.fetch(anyInt(), anyLong()))
                .thenReturn(List.of(message))
                .thenReturn(List.of());
        subscriber = subscriber(m -> {
            sleep(200);
            return StatusResult.success();
        });

        subscriber.startConsuming(subscription);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verify(message).ack());
        verify(message, atLeast(1)).inProgress();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void shouldFinishCurrentMessageAndReleaseTheOthers_whenStopped() throws InterruptedException {
        var current = messageDeliveredTimes(1);
        var waiting = messageDeliveredTimes(1);
        when(subscription.fetch(anyInt(), anyLong()))
                .thenReturn(List.of(current, waiting))
                .thenReturn(List.of());
        var processing = new CountDownLatch(1);
        subscriber = subscriber(m -> {
            processing.countDown();
            sleep(300);
            return StatusResult.success();
        });
        subscriber.startConsuming(subscription);
        assertThat(processing.await(5, TimeUnit.SECONDS)).isTrue();

        subscriber.stop();

        verify(current).ack();
        verify(waiting).nak();
        verify(waiting, never()).ack();
        assertThat(handled).containsExactly(current);
    }

    @Test
    void shouldNotWaitLongerThanShutdownTimeout_whenStopped() throws InterruptedException {
        var message = messageDeliveredTimes(1);
        when(subscription.fetch(anyInt(), anyLong()))
                .thenReturn(List.of(message))
                .thenReturn(List.of());
        var processing = new CountDownLatch(1);
        Function<Message, StatusResult<Void>> slowHandler = m -> {
            processing.countDown();
            sleep(5_000);
            return StatusResult.success();
        };
        subscriber = TestSubscriber.Builder.newInstance(slowHandler, handled)
                .url("nats://localhost:4222")
                .stream("stream")
                .name("test")
                .subject("test.>")
                .monitor(monitor)
                .shutdownTimeout(Duration.ofMillis(200))
                .build();
        subscriber.startConsuming(subscription);
        assertThat(processing.await(5, TimeUnit.SECONDS)).isTrue();

        var start = System.currentTimeMillis();
        subscriber.stop();

        assertThat(System.currentTimeMillis() - start).isLessThan(3_000);
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
                .retryBaseDelayMillis(RETRY_BASE_DELAY)
                .inProgressInterval(Duration.ofMillis(50))
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
