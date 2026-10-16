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
import io.nats.client.Options;
import io.nats.client.PullSubscribeOptions;
import io.nats.client.api.StorageType;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.eclipse.edc.nats.NatsFunctions.createConsumer;
import static org.eclipse.edc.nats.NatsFunctions.createStream;

public abstract class NatsSubscriber {

    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);
    protected String url;
    protected String stream;
    protected String name;
    protected String subject;
    protected ExecutorService executorService;
    protected Monitor monitor;
    protected boolean autoCreateStream = false;
    protected boolean autoCreateConsumer = false;
    protected Integer batchSize = 100;
    protected Integer maxWait = 100;
    protected long errorBackoffMillis = 1000;
    // externally contributed connect options (e.g. NKey authentication); the server URL is overwritten
    protected Options authenticationOptions;
    private Connection connection;


    public boolean isAutoCreateStream() {
        return autoCreateStream;
    }

    public boolean isAutoCreateConsumer() {
        return autoCreateConsumer;
    }

    public void prepare() {
        if (!autoCreateStream && !autoCreateConsumer) {
            return;
        }
        try {
            var conn = getOrCreateConnection();
            conn.jetStream();
            var jsm = conn.jetStreamManagement();
            if (autoCreateStream) {
                createStream(jsm, stream, StorageType.Memory, subject);
            }
            if (autoCreateConsumer) {
                createConsumer(jsm, stream, name, subject);
            }
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
            startConsuming(sub);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start NATS subscriber", e);
        }

    }

    /**
     * Starts consuming the messages of the given subscription on the subscriber thread.
     *
     * @param sub the subscription.
     */
    void startConsuming(JetStreamSubscription sub) {
        active.set(true);
        started.set(true);
        running.set(true);
        executorService.submit(() -> {
            run(sub);
        });
    }

    private Connection getOrCreateConnection() {
        if (connection == null) {
            try {
                var builder = authenticationOptions != null ? new Options.Builder(authenticationOptions) : new Options.Builder();
                connection = Nats.connect(builder.server(url).build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to create NATS connection", e);
            }
        }
        return connection;
    }

    /**
     * Tells if the subscriber is consuming messages. A subscriber that has not been started yet is considered healthy.
     * Once started, it is healthy while its consumer loop runs and its connection is not closed for good, i.e. it has
     * not given up reconnecting.
     *
     * @return true if the subscriber is healthy, false otherwise.
     */
    public boolean isHealthy() {
        if (!started.get()) {
            return true;
        }
        return running.get() && (connection == null || connection.getStatus() != Connection.Status.CLOSED);
    }

    protected abstract StatusResult<Void> handleMessage(Message message);

    private void run(JetStreamSubscription sub) {
        try {
            while (active.get()) {
                try {
                    consume(sub.fetch(batchSize, maxWait));
                } catch (Exception e) {
                    if (!active.get()) {
                        return;
                    }
                    monitor.severe("NATS subscriber %s: failed to consume messages, retrying in %d ms".formatted(name, errorBackoffMillis), e);
                    pause();
                }
            }
        } catch (Throwable e) {
            monitor.severe("NATS subscriber %s: unrecoverable error, the subscriber stopped consuming messages".formatted(name), e);
        } finally {
            running.set(false);
        }
    }

    private void consume(List<Message> messages) {
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
                nakQuietly(message);
            }
        }
    }

    private void nakQuietly(Message message) {
        try {
            message.nak();
        } catch (Exception e) {
            // the message is redelivered once its ack wait expires
            monitor.warning("NATS subscriber %s: cannot nak the message: %s".formatted(name, e.getMessage()));
        }
    }

    private void pause() {
        try {
            Thread.sleep(errorBackoffMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            active.set(false);
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

        public B autoCreateStream(boolean autoCreateStream) {
            subscriber.autoCreateStream = autoCreateStream;
            return self();
        }

        public B autoCreateConsumer(boolean autoCreateConsumer) {
            subscriber.autoCreateConsumer = autoCreateConsumer;
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

        public B errorBackoffMillis(long errorBackoffMillis) {
            subscriber.errorBackoffMillis = errorBackoffMillis;
            return self();
        }

        public B authenticationOptions(Options authenticationOptions) {
            subscriber.authenticationOptions = authenticationOptions;
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
