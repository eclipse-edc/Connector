/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.dataplane.kafka.pipeline;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

public class KafkaDataSource implements DataSource {

    private String name;
    private Monitor monitor;
    private Duration pollDuration;
    private Duration maxDuration;
    private Consumer<String, byte[]> consumer;
    private Clock clock;
    private final AtomicBoolean active = new AtomicBoolean(true);

    private KafkaDataSource() {
    }

    @Override
    public void close() {
        active.set(false);
    }

    @Override
    public StreamResult<Stream<Part>> openPartStream() {
        var stream = openRecordsStream()
                .flatMap(consumerRecords -> consumerRecords.partitions().stream()
                        .flatMap(p -> consumerRecords.records(p).stream())
                        .map(KafkaPart::new)
                        .map(Part.class::cast))
                .onClose(() -> consumer.close());

        return StreamResult.success(stream);
    }

    @NotNull
    private Stream<ConsumerRecords<String, byte[]>> openRecordsStream() {
        return stream(spliteratorUnknownSize(new ConsumerRecordsIterator(), 0), /* not parallel */ false);
    }

    public static class Builder {

        private Properties consumerProperties;
        private String topic;
        private final KafkaDataSource dataSource;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder name(String name) {
            dataSource.name = name;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            dataSource.monitor = monitor;
            return this;
        }

        public Builder clock(Clock clock) {
            dataSource.clock = clock;
            return this;
        }

        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }

        public Builder pollDuration(Duration pollDuration) {
            dataSource.pollDuration = pollDuration;
            return this;
        }

        public Builder maxDuration(Duration maxDuration) {
            dataSource.maxDuration = maxDuration;
            return this;
        }

        public Builder consumerProperties(Properties consumerProperties) {
            this.consumerProperties = consumerProperties;
            return this;
        }

        public KafkaDataSource build() {
            Objects.requireNonNull(dataSource.monitor, "monitor");
            Objects.requireNonNull(dataSource.pollDuration, "pollDuration");
            Objects.requireNonNull(topic, "topic");
            Objects.requireNonNull(consumerProperties, "consumerProperties");
            Objects.requireNonNull(dataSource.clock, "clock");

            dataSource.consumer = new KafkaConsumer<>(consumerProperties);
            dataSource.consumer.subscribe(List.of(topic));

            return dataSource;
        }

        private Builder() {
            dataSource = new KafkaDataSource();
        }
    }

    private class KafkaPart implements Part {

        private final ConsumerRecord<String, byte[]> consumerRecord;

        private KafkaPart(ConsumerRecord<String, byte[]> consumerRecord) {
            this.consumerRecord = consumerRecord;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public InputStream openStream() {
            return new ByteArrayInputStream(consumerRecord.value());
        }
    }

    private class ConsumerRecordsIterator implements Iterator<ConsumerRecords<String, byte[]>> {

        private final Instant streamEnd;

        ConsumerRecordsIterator() {
            this.streamEnd = maxDuration == null ? Instant.MAX : clock.instant().plus(maxDuration);
            debug("starts consuming events until: " + streamEnd);
        }

        @Override
        public boolean hasNext() {
            var isMaxDurationReached = clock.instant().isAfter(streamEnd);
            if (isMaxDurationReached) {
                debug("max duration reached");
            }
            return active.get() && !isMaxDurationReached;
        }

        @Override
        public ConsumerRecords<String, byte[]> next() {
            var records = consumer.poll(Duration.ZERO);
            while (records.isEmpty() && active.get()) {
                records = consumer.poll(pollDuration);
            }
            return records;
        }

        private void debug(String message) {
            monitor.debug(String.format("KafkaDataSource %s %s", name, message));
        }
    }
}
