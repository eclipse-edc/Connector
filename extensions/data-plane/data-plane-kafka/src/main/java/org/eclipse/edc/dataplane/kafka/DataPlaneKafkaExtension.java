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
import org.eclipse.edc.connector.dataplane.spi.pipeline.TransferService;
import org.eclipse.edc.dataplane.kafka.config.KafkaPropertiesFactory;
import org.eclipse.edc.dataplane.kafka.pipeline.KafkaDataSinkFactory;
import org.eclipse.edc.dataplane.kafka.pipeline.KafkaDataSourceFactory;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.time.Clock;

@Extension(value = DataPlaneKafkaExtension.NAME)
public class DataPlaneKafkaExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Kafka";

    @Inject
    private DataTransferExecutorServiceContainer executorContainer;

    @Inject
    private TransferService transferService;

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

        transferService.registerFactory(new KafkaDataSourceFactory(monitor, propertiesFactory, clock));
        transferService.registerFactory(new KafkaDataSinkFactory(executorContainer.getExecutorService(), monitor, propertiesFactory));
    }
}
