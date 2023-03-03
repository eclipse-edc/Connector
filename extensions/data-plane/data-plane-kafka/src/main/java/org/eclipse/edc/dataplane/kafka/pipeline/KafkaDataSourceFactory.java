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

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.connector.dataplane.util.validation.ValidationRule;
import org.eclipse.edc.dataplane.kafka.config.KafkaPropertiesFactory;
import org.eclipse.edc.dataplane.kafka.pipeline.validation.KafkaSourceDataAddressValidationRule;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

import static java.lang.String.format;
import static org.eclipse.edc.dataplane.kafka.schema.KafkaDataAddressSchema.KAFKA_TYPE;
import static org.eclipse.edc.dataplane.kafka.schema.KafkaDataAddressSchema.MAX_DURATION;
import static org.eclipse.edc.dataplane.kafka.schema.KafkaDataAddressSchema.NAME;
import static org.eclipse.edc.dataplane.kafka.schema.KafkaDataAddressSchema.POLL_DURATION;
import static org.eclipse.edc.dataplane.kafka.schema.KafkaDataAddressSchema.TOPIC;

public class KafkaDataSourceFactory implements DataSourceFactory {

    private static final Duration DEFAULT_POLL_DURATION = Duration.ofSeconds(1);

    private final Monitor monitor;
    private final ValidationRule<DataAddress> validation;
    private final KafkaPropertiesFactory propertiesFactory;
    private final Clock clock;

    public KafkaDataSourceFactory(Monitor monitor, KafkaPropertiesFactory propertiesFactory, Clock clock) {
        this.monitor = monitor;
        this.propertiesFactory = propertiesFactory;
        this.validation = new KafkaSourceDataAddressValidationRule(propertiesFactory);
        this.clock = clock;
    }

    @Override
    public boolean canHandle(DataFlowRequest dataRequest) {
        return KAFKA_TYPE.equalsIgnoreCase(dataRequest.getSourceDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        var source = request.getSourceDataAddress();
        return validation.apply(source).map(it -> true);
    }

    @Override
    public DataSource createSource(DataFlowRequest request) {
        var validationResult = validate(request);
        if (validationResult.failed()) {
            throw new EdcException(validationResult.getFailureDetail());
        }

        var source = request.getSourceDataAddress();
        var groupId = request.getProcessId() + ":" + request.getId();

        var consumerProps = propertiesFactory.getConsumerProperties(source.getProperties())
                .orElseThrow(failure -> new IllegalArgumentException(failure.getFailureDetail()));
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        var topic = Optional.ofNullable(source.getProperties().get(TOPIC))
                .orElseThrow(() -> new IllegalArgumentException(format("Missing `%s` config", TOPIC)));

        var name = source.getProperties().get(NAME);

        var maxDuration = Optional.ofNullable(source.getProperties().get(MAX_DURATION))
                .map(Duration::parse)
                .orElse(null);

        var pollDuration = Optional.ofNullable(source.getProperties().get(POLL_DURATION))
                .map(Duration::parse)
                .orElse(DEFAULT_POLL_DURATION);

        return KafkaDataSource.Builder.newInstance()
                .monitor(monitor)
                .clock(clock)
                .topic(topic)
                .name(name)
                .pollDuration(pollDuration)
                .maxDuration(maxDuration)
                .consumerProperties(consumerProps)
                .build();
    }
}
