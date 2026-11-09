/*
 *  Copyright (c) 2026 Matthias Buchhorn
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Matthias Buchhorn - initial API and implementation
 *
 */

package org.eclipse.edc.nats.subscriber;

import io.nats.client.JetStreamManagement;
import io.nats.client.Message;
import io.nats.client.Nats;
import org.eclipse.edc.nats.testfixtures.NatsEndToEndExtension;
import org.eclipse.edc.spi.response.StatusResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.nats.NatsFunctions.streamExists;
import static org.mockito.Mockito.mock;

/**
 * The health of a started subscriber against a real NATS server, when its stream or its consumer goes away, as after a
 * restart of a NATS server that keeps them in memory.
 */
class NatsSubscriberHealthTest {

    private static final String STREAM_NAME = "health_test_stream";
    private static final String CONSUMER_NAME = "health_test_consumer";
    private static final String SUBJECT = "health.test.>";

    @Order(0)
    @RegisterExtension
    static final NatsEndToEndExtension NATS_EXTENSION = new NatsEndToEndExtension();

    private static JetStreamManagement jsm;

    private final List<TestSubscriber> subscribers = new ArrayList<>();

    @BeforeAll
    static void beforeAll() throws Exception {
        jsm = Nats.connect(NATS_EXTENSION.getNatsUrl()).jetStreamManagement();
    }

    @AfterEach
    void afterEach() {
        subscribers.forEach(TestSubscriber::stop);
        subscribers.clear();
        if (streamExists(jsm, STREAM_NAME)) {
            NATS_EXTENSION.deleteStream(STREAM_NAME);
        }
    }

    @Test
    void isHealthy_shouldBeTrue_whenStreamAndConsumerExist() {
        var subscriber = startedSubscriber(Duration.ZERO);

        assertThat(subscriber.isHealthy()).isTrue();
    }

    @Test
    void isHealthy_shouldBeFalse_whenStreamIsDeleted() {
        var subscriber = startedSubscriber(Duration.ZERO);

        NATS_EXTENSION.deleteStream(STREAM_NAME);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(subscriber.isHealthy()).isFalse());
    }

    @Test
    void isHealthy_shouldBeFalse_whenConsumerIsDeleted() throws Exception {
        var subscriber = startedSubscriber(Duration.ZERO);

        jsm.deleteConsumer(STREAM_NAME, CONSUMER_NAME);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(subscriber.isHealthy()).isFalse());
    }

    @Test
    void isHealthy_shouldBeTrueAgain_whenStreamAndConsumerAreRecreated() {
        var subscriber = startedSubscriber(Duration.ZERO);
        NATS_EXTENSION.deleteStream(STREAM_NAME);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(subscriber.isHealthy()).isFalse());

        NATS_EXTENSION.createStream(STREAM_NAME, SUBJECT);
        NATS_EXTENSION.createConsumer(STREAM_NAME, CONSUMER_NAME, SUBJECT);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(subscriber.isHealthy()).isTrue());
    }

    @Test
    void isHealthy_shouldKeepTheLastAnswer_withinTheCheckInterval() {
        var subscriber = startedSubscriber(Duration.ofHours(1));
        assertThat(subscriber.isHealthy()).isTrue();

        NATS_EXTENSION.deleteStream(STREAM_NAME);

        assertThat(subscriber.isHealthy()).isTrue();
    }

    private TestSubscriber startedSubscriber(Duration healthCheckInterval) {
        var subscriber = TestSubscriber.Builder.newInstance()
                .url(NATS_EXTENSION.getNatsUrl())
                .name(CONSUMER_NAME)
                .stream(STREAM_NAME)
                .subject(SUBJECT)
                .autoCreateStream(true)
                .autoCreateConsumer(true)
                .healthCheckInterval(healthCheckInterval)
                .monitor(mock())
                .build();
        subscribers.add(subscriber);
        subscriber.prepare();
        subscriber.start();
        return subscriber;
    }

    private static class TestSubscriber extends NatsSubscriber {

        @Override
        protected StatusResult<Void> handleMessage(Message message) {
            return StatusResult.success();
        }

        static class Builder extends NatsSubscriber.Builder<TestSubscriber, Builder> {

            protected Builder(TestSubscriber subscriber) {
                super(subscriber);
            }

            static Builder newInstance() {
                return new Builder(new TestSubscriber());
            }

            @Override
            public Builder self() {
                return this;
            }
        }
    }
}
