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

package org.eclipse.edc.connector.dataplane.kafka.pipeline;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.util.sink.ParallelSink;
import org.eclipse.edc.spi.result.AbstractResult;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

class KafkaDataSink extends ParallelSink implements Closeable {

    private String topic;
    private Producer<String, byte[]> producer;

    private KafkaDataSink() {
    }

    @Override
    public void close() {
        if (producer != null) {
            producer.close();
        }
    }

    @Override
    protected StreamResult<Object> transferParts(List<DataSource.Part> parts) {
        return parts.stream()
                .map(this::publishPart)
                .filter(AbstractResult::failed)
                .findFirst()
                .orElse(StreamResult.success());
    }

    private StreamResult<Object> publishPart(DataSource.Part part) {
        try (var is = part.openStream()) {
            // asynchronous publishing
            producer.send(new ProducerRecord<>(topic, null, is.readAllBytes()), (metadata, exception) -> {
                if (exception != null) {
                    monitor.warning("Failed to publish message:  " + metadata, exception);
                }
            });
            return StreamResult.success();
        } catch (IOException e) {
            return StreamResult.error("Failed to open part with name: " + part.name());
        }
    }

    public static class Builder extends ParallelSink.Builder<Builder, KafkaDataSink> {

        private Properties producerProperties;

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new KafkaDataSink());
        }

        public Builder topic(String topic) {
            sink.topic = topic;
            return this;
        }

        public Builder producerProperties(Properties producerProperties) {
            this.producerProperties = producerProperties;
            return this;
        }

        @Override
        protected void validate() {
            Objects.requireNonNull(sink.monitor, "monitor");
            Objects.requireNonNull(sink.topic, "topic");
            Objects.requireNonNull(producerProperties, "producerProperties");

            sink.producer = new KafkaProducer<>(producerProperties);
        }
    }
}
