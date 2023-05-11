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

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.dataplane.kafka.config.KafkaPropertiesFactory;
import org.eclipse.edc.dataplane.kafka.pipeline.validation.KafkaSinkDataAddressValidation;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static java.lang.String.format;
import static org.eclipse.edc.dataplane.kafka.schema.KafkaDataAddressSchema.KAFKA_TYPE;
import static org.eclipse.edc.dataplane.kafka.schema.KafkaDataAddressSchema.TOPIC;

public class KafkaDataSinkFactory implements DataSinkFactory {

    private final ExecutorService executorService;
    private final Monitor monitor;
    private final KafkaPropertiesFactory propertiesFactory;
    private final KafkaSinkDataAddressValidation validation;

    public KafkaDataSinkFactory(ExecutorService executorService, Monitor monitor, KafkaPropertiesFactory propertiesFactory) {
        this.executorService = executorService;
        this.monitor = monitor;
        this.propertiesFactory = propertiesFactory;
        this.validation = new KafkaSinkDataAddressValidation(propertiesFactory);
    }

    @Override
    public boolean canHandle(DataFlowRequest dataRequest) {
        return KAFKA_TYPE.equalsIgnoreCase(dataRequest.getDestinationDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        var destination = request.getDestinationDataAddress();
        return validation.apply(destination).map(it -> true);
    }

    @Override
    public DataSink createSink(DataFlowRequest request) {
        var validationResult = validate(request);
        if (validationResult.failed()) {
            throw new EdcException(validationResult.getFailureDetail());
        }

        var destination = request.getDestinationDataAddress();
        var producerProps = propertiesFactory.getProducerProperties(destination.getProperties())
                .orElseThrow(failure -> new IllegalArgumentException(failure.getFailureDetail()));

        var topic = Optional.ofNullable(destination.getProperties().get(TOPIC))
                .orElseThrow(() -> new IllegalArgumentException(format("Missing `%s` config", TOPIC)));

        return KafkaDataSink.Builder.newInstance()
                .monitor(monitor)
                .requestId(request.getId())
                .topic(topic)
                .producerProperties(producerProps)
                .executorService(executorService)
                .build();
    }
}
