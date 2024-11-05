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

import org.eclipse.edc.connector.dataplane.kafka.config.KafkaPropertiesFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.validator.dataaddress.kafka.KafkaDataAddressValidator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import static org.eclipse.edc.dataaddress.kafka.spi.KafkaDataAddressSchema.KAFKA_TYPE;
import static org.eclipse.edc.dataaddress.kafka.spi.KafkaDataAddressSchema.TOPIC;

public class KafkaDataSinkFactory implements DataSinkFactory {

    private final ExecutorService executorService;
    private final Monitor monitor;
    private final KafkaPropertiesFactory propertiesFactory;
    private final Validator<DataAddress> validation;
    private final int partitionSize;

    public KafkaDataSinkFactory(ExecutorService executorService, Monitor monitor, KafkaPropertiesFactory propertiesFactory, int partitionSize) {
        this.executorService = executorService;
        this.monitor = monitor;
        this.propertiesFactory = propertiesFactory;
        this.validation = new KafkaDataAddressValidator();
        this.partitionSize = partitionSize;
    }

    @Override
    public String supportedType() {
        return KAFKA_TYPE;
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowStartMessage request) {
        var destination = request.getDestinationDataAddress();
        return validation.validate(destination).flatMap(ValidationResult::toResult);
    }

    @Override
    public DataSink createSink(DataFlowStartMessage request) {
        var validationResult = validateRequest(request);
        if (validationResult.failed()) {
            throw new EdcException(validationResult.getFailureDetail());
        }

        var destination = request.getDestinationDataAddress();
        var producerProps = propertiesFactory.getProducerProperties(destination.getProperties())
                .orElseThrow(failure -> new IllegalArgumentException(failure.getFailureDetail()));

        return KafkaDataSink.Builder.newInstance()
                .monitor(monitor)
                .requestId(request.getId())
                .topic(destination.getStringProperty(TOPIC))
                .producerProperties(producerProps)
                .partitionSize(partitionSize)
                .executorService(executorService)
                .build();
    }
}
