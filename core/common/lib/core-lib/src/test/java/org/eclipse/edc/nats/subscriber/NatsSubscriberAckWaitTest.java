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

import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.api.ConsumerConfiguration;
import org.eclipse.edc.nats.testfixtures.NatsEndToEndExtension;
import org.eclipse.edc.spi.response.StatusResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

/**
 * Two replicas share the same durable consumer, whose ack wait is shorter than the time needed to process a batch: a
 * message held by a replica must not be delivered to the other one while it is being processed.
 */
class NatsSubscriberAckWaitTest {

    private static final String STREAM_NAME = "ack_wait_test_stream";
    private static final String CONSUMER_NAME = "ack_wait_test_consumer";
    private static final String SUBJECT = "ack.wait.>";
    private static final int MESSAGES = 4;

    @Order(0)
    @RegisterExtension
    static final NatsEndToEndExtension NATS_EXTENSION = new NatsEndToEndExtension();

    private final Map<String, List<String>> handledBy = new ConcurrentHashMap<>();
    private final List<SlowSubscriber> subscribers = new ArrayList<>();

    @AfterEach
    void afterEach() {
        subscribers.forEach(NatsSubscriber::stop);
        NATS_EXTENSION.deleteStream(STREAM_NAME);
    }

    @Test
    void shouldNotDeliverHeldMessageToAnotherReplica_whenProcessingExceedsAckWait() throws Exception {
        NATS_EXTENSION.createStream(STREAM_NAME, SUBJECT);
        try (var connection = Nats.connect(NATS_EXTENSION.getNatsUrl())) {
            connection.jetStreamManagement().addOrUpdateConsumer(STREAM_NAME, ConsumerConfiguration.builder()
                    .durable(CONSUMER_NAME).name(CONSUMER_NAME).filterSubject(SUBJECT).ackWait(Duration.ofSeconds(2)).build());
        }

        for (var i = 1; i <= MESSAGES; i++) {
            NATS_EXTENSION.publish("ack.wait.task", ("task-" + i).getBytes());
        }

        // replica A fetches all the messages: processing them takes 6 s (1.5 s each), longer than the 2 s ack wait.
        // Replica B starts afterwards, so it can only get messages that are redelivered while A still holds them
        subscriber("replica-a").start();
        await().atMost(Duration.ofSeconds(5)).until(() -> !handledBy.isEmpty());
        subscriber("replica-b").start();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(handledBy).hasSize(MESSAGES));
        await().pollDelay(Duration.ofSeconds(4)).untilAsserted(() ->
                assertThat(handledBy.values()).allSatisfy(replicas -> assertThat(replicas).containsExactly("replica-a")));
    }

    private SlowSubscriber subscriber(String replica) {
        var subscriber = SlowSubscriber.Builder.newInstance(replica, handledBy)
                .url(NATS_EXTENSION.getNatsUrl())
                .name(CONSUMER_NAME)
                .stream(STREAM_NAME)
                .subject(SUBJECT)
                .batchSize(MESSAGES)
                .monitor(mock())
                .build();
        subscribers.add(subscriber);
        return subscriber;
    }

    private static class SlowSubscriber extends NatsSubscriber {

        private String replica;
        private Map<String, List<String>> handledBy;

        @Override
        protected StatusResult<Void> handleMessage(Message message) {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            handledBy.computeIfAbsent(new String(message.getData()), k -> new CopyOnWriteArrayList<>()).add(replica);
            return StatusResult.success();
        }

        static class Builder extends NatsSubscriber.Builder<SlowSubscriber, Builder> {

            protected Builder(SlowSubscriber subscriber) {
                super(subscriber);
            }

            static Builder newInstance(String replica, Map<String, List<String>> handledBy) {
                var subscriber = new SlowSubscriber();
                subscriber.replica = replica;
                subscriber.handledBy = handledBy;
                return new Builder(subscriber);
            }

            @Override
            public Builder self() {
                return this;
            }
        }
    }
}
