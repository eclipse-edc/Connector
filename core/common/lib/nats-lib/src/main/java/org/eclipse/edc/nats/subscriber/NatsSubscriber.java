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

import io.nats.client.Connection;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.PullSubscribeOptions;
import io.nats.client.api.StorageType;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.eclipse.edc.nats.NatsFunctions.createConsumer;
import static org.eclipse.edc.nats.NatsFunctions.createStream;

public abstract class NatsSubscriber {

    private final AtomicBoolean active = new AtomicBoolean(false);
    protected String url;
    protected String stream;
    protected String name;
    protected String subject;
    protected ExecutorService executorService;
    protected Monitor monitor;
    protected boolean autoCreate = false;
    protected Integer batchSize = 100;
    protected Integer maxWait = 100;
    private Connection connection;


    public void prepare() {
        if (!autoCreate) {
            return;
        }
        try {
            var conn = getOrCreateConnection();
            conn.jetStream();
            var jsm = conn.jetStreamManagement();
            createStream(jsm, stream, StorageType.Memory, subject);
            createConsumer(jsm, stream, name, subject);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        try {
            var connection = getOrCreateConnection();
            var js = connection.jetStream();
            var pullOptions = PullSubscribeOptions.builder()
                    .stream(stream)
                    .durable(name)
                    .build();

            var sub = js.subscribe(subject, pullOptions);
            active.set(true);

            executorService.submit(() -> {
                run(sub);
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to start NATS subscriber", e);
        }

    }

    private Connection getOrCreateConnection() {
        if (connection == null) {
            try {
                connection = Nats.connect(url);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create NATS connection", e);
            }
        }
        return connection;
    }

    protected abstract StatusResult<Void> handleMessage(Message message);

    private void run(JetStreamSubscription sub) {
        while (active.get()) {
            var messages = sub.fetch(batchSize, maxWait);
            for (var message : messages) {
                try {
                    var result = handleMessage(message);
                    if (result.failed()) {
                        if (result.fatalError()) {
                            monitor.severe("Failed to handle Nats message, received a fatal error: " + result.getFailureMessages());
                            message.term();
                        } else {
                            monitor.warning("Failed to handle Nats message: " + result.getFailureMessages());
                            message.nak();
                        }
                        continue;
                    }
                    message.ack();
                } catch (Exception e) {
                    monitor.severe("Failed to process transfer message: " + e.getMessage(), e);
                    message.nak();
                }
            }
        }
    }

    public void stop() {
        active.set(false);
        executorService.shutdown();
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract static class Builder<T extends NatsSubscriber, B extends Builder<T, B>> {

        protected final T subscriber;
        private ExecutorInstrumentation executorInstrumentation = ExecutorInstrumentation.noop();

        protected Builder(T subscriber) {
            this.subscriber = subscriber;
        }

        public abstract B self();

        public B url(String url) {
            subscriber.url = url;
            return self();
        }

        public B stream(String stream) {
            subscriber.stream = stream;
            return self();
        }

        public B name(String name) {
            subscriber.name = name;
            return self();
        }

        public B subject(String subject) {
            subscriber.subject = subject;
            return self();
        }

        public B executorInstrumentation(ExecutorInstrumentation executorInstrumentation) {
            this.executorInstrumentation = executorInstrumentation;
            return self();
        }

        public B monitor(Monitor monitor) {
            subscriber.monitor = monitor;
            return self();
        }

        public B autoCreate(boolean autoCreate) {
            subscriber.autoCreate = autoCreate;
            return self();
        }

        public B batchSize(int batchSize) {
            subscriber.batchSize = batchSize;
            return self();
        }

        public B maxWait(int maxWait) {
            subscriber.maxWait = maxWait;
            return self();
        }

        public T build() {

            Objects.requireNonNull(subscriber.url, "url");
            Objects.requireNonNull(subscriber.stream, "stream");
            Objects.requireNonNull(subscriber.name, "name");
            Objects.requireNonNull(subscriber.subject, "subject");
            Objects.requireNonNull(executorInstrumentation, "executorInstrumentation");
            Objects.requireNonNull(subscriber.monitor, "monitor");

            var name = "NatsSubscriber-" + subscriber.name;
            subscriber.executorService = executorInstrumentation.instrument(Executors.newSingleThreadScheduledExecutor(r -> {
                var thread = Executors.defaultThreadFactory().newThread(r);
                thread.setName(name);
                return thread;
            }), name);

            return subscriber;
        }
    }
}
