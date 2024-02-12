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

package org.eclipse.edc.dataplane.kafka;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataTransferExecutorServiceContainer;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.dataplane.kafka.config.KafkaPropertiesFactory;
import org.eclipse.edc.dataplane.kafka.pipeline.KafkaDataSinkFactory;
import org.eclipse.edc.dataplane.kafka.pipeline.KafkaDataSourceFactory;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.time.Clock;

@Extension(value = DataPlaneKafkaExtension.NAME)
public class DataPlaneKafkaExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Kafka";

    private static final int DEFAULT_PART_SIZE = 5;

    @Setting
    private static final String EDC_DATAPLANE_KAFKA_SINK_PARTITION_SIZE = "edc.dataplane.kafka.sink.partition.size";

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

        var sinkPartitionSize = context.getSetting(EDC_DATAPLANE_KAFKA_SINK_PARTITION_SIZE, DEFAULT_PART_SIZE);

        pipelineService.registerFactory(new KafkaDataSourceFactory(monitor, propertiesFactory, clock));
        pipelineService.registerFactory(new KafkaDataSinkFactory(executorContainer.getExecutorService(), monitor, propertiesFactory, sinkPartitionSize));
    }
}
