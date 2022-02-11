/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.dataplane.framework;

import org.eclipse.dataspaceconnector.dataplane.framework.manager.DataPlaneManagerImpl;
import org.eclipse.dataspaceconnector.dataplane.framework.pipeline.PipelineServiceImpl;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * Provides core services for the Data Plane Framework.
 */
@Provides({DataPlaneManager.class, PipelineService.class})
public class DataPlaneFrameworkExtension implements ServiceExtension {

    @EdcSetting
    private static final String QUEUE_CAPACITY = "edc.dataplane.queue.capacity";
    private static final int DEFAULT_QUEUE_CAPACITY = 10000;

    @EdcSetting
    private static final String WORKERS = "edc.dataplane.workers";
    private static final int DEFAULT_WORKERS = 10;

    @EdcSetting
    private static final String WAIT_TIMEOUT = "edc.dataplane.wait";
    private static final long DEFAULT_WAIT_TIMEOUT = 1000;

    private DataPlaneManagerImpl dataPlaneManager;

    @Override
    public String name() {
        return "Data Plane Framework";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var pipelineService = new PipelineServiceImpl();
        context.registerService(PipelineService.class, pipelineService);

        var monitor = context.getMonitor();
        var queueCapacity = context.getSetting(QUEUE_CAPACITY, DEFAULT_QUEUE_CAPACITY);
        var workers = context.getSetting(WORKERS, DEFAULT_WORKERS);
        var waitTimeout = context.getSetting(WAIT_TIMEOUT, DEFAULT_WAIT_TIMEOUT);

        dataPlaneManager = DataPlaneManagerImpl.Builder.newInstance()
                .queueCapacity(queueCapacity)
                .workers(workers)
                .waitTimeout(waitTimeout)
                .pipelineService(pipelineService)
                .monitor(monitor).build();

        context.registerService(DataPlaneManager.class, dataPlaneManager);
    }

    @Override
    public void start() {
        dataPlaneManager.start();
    }

    @Override
    public void shutdown() {
        if (dataPlaneManager != null) {
            dataPlaneManager.forceStop();
        }
    }
}
