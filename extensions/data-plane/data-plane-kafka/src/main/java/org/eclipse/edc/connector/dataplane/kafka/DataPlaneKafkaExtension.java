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

package org.eclipse.edc.connector.dataplane.kafka;

import org.eclipse.edc.connector.dataplane.kafka.config.KafkaPropertiesFactory;
import org.eclipse.edc.connector.dataplane.kafka.pipeline.KafkaDataSinkFactory;
import org.eclipse.edc.connector.dataplane.kafka.pipeline.KafkaDataSourceFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataTransferExecutorServiceContainer;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.time.Clock;

@Extension(value = DataPlaneKafkaExtension.NAME)
public class DataPlaneKafkaExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Kafka";

    private static final int DEFAULT_PARTITION_SIZE = 5;

    @Setting(description = "The partitionSize used by the kafka data sink", defaultValue = DEFAULT_PARTITION_SIZE + "", min = 1, key = "edc.dataplane.kafka.sink.partition.size")
    private int partitionSize;

    @Inject
    private DataTransferExecutorServiceContainer executorContainer;

    @Inject
    private PipelineService pipelineService;

    @Inject
    private Clock clock;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        var propertiesFactory = new KafkaPropertiesFactory();


        pipelineService.registerFactory(new KafkaDataSourceFactory(monitor, propertiesFactory, clock));
        pipelineService.registerFactory(new KafkaDataSinkFactory(executorContainer.getExecutorService(), monitor, propertiesFactory, partitionSize));
    }
}
