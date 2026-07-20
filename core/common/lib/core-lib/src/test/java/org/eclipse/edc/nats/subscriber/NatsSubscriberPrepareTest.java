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

import io.nats.client.JetStreamApiException;
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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.nats.NatsFunctions.streamExists;
import static org.mockito.Mockito.mock;

class NatsSubscriberPrepareTest {

    private static final String STREAM_NAME = "prepare_test_stream";
    private static final String CONSUMER_NAME = "prepare_test_consumer";
    private static final String SUBJECT = "prepare.test.>";

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
    void prepare_shouldNotCreateAnything_whenBothFlagsDisabled() {
        subscriber(false, false).prepare();

        assertThat(streamExists(jsm, STREAM_NAME)).isFalse();
    }

    @Test
    void prepare_shouldCreateStreamOnly_whenOnlyStreamEnabled() {
        subscriber(true, false).prepare();

        assertThat(streamExists(jsm, STREAM_NAME)).isTrue();
        assertThat(consumerExists()).isFalse();
    }

    @Test
    void prepare_shouldCreateConsumerOnly_whenOnlyConsumerEnabled() {
        NATS_EXTENSION.createStream(STREAM_NAME, SUBJECT);

        subscriber(false, true).prepare();

        assertThat(consumerExists()).isTrue();
    }

    @Test
    void prepare_shouldNotCreateStream_whenOnlyConsumerEnabledAndStreamMissing() {
        var subscriber = subscriber(false, true);

        assertThat(streamExists(jsm, STREAM_NAME)).isFalse();
        // creating a consumer on a non-existing stream is an error, the stream must be provisioned externally
        assertThatThrownBy(subscriber::prepare).isInstanceOf(RuntimeException.class);
        assertThat(streamExists(jsm, STREAM_NAME)).isFalse();
    }

    @Test
    void prepare_shouldCreateBoth_whenBothFlagsEnabled() {
        subscriber(true, true).prepare();

        assertThat(streamExists(jsm, STREAM_NAME)).isTrue();
        assertThat(consumerExists()).isTrue();
    }

    private boolean consumerExists() {
        try {
            return jsm.getConsumerInfo(STREAM_NAME, CONSUMER_NAME) != null;
        } catch (JetStreamApiException e) {
            if (e.getErrorCode() == 404) {
                return false;
            }
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TestSubscriber subscriber(boolean autoCreateStream, boolean autoCreateConsumer) {
        var subscriber = TestSubscriber.Builder.newInstance()
                .url(NATS_EXTENSION.getNatsUrl())
                .name(CONSUMER_NAME)
                .stream(STREAM_NAME)
                .subject(SUBJECT)
                .autoCreateStream(autoCreateStream)
                .autoCreateConsumer(autoCreateConsumer)
                .monitor(mock())
                .build();
        subscribers.add(subscriber);
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
