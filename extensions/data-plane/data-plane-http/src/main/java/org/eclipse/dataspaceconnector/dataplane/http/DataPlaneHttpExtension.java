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
package org.eclipse.dataspaceconnector.dataplane.http;

import net.jodah.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.dataplane.http.pipeline.HttpDataSinkFactory;
import org.eclipse.dataspaceconnector.dataplane.http.pipeline.HttpDataSourceFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.concurrent.Executors;

/**
 * Provides support for reading data from an HTTP endpoint and sending data to an HTTP endpoint.
 */
public class DataPlaneHttpExtension implements ServiceExtension {

    @Inject
    private OkHttpClient httpClient;

    @Inject
    @SuppressWarnings("rawtypes")
    private RetryPolicy retryPolicy;

    @Inject
    private PipelineService pipelineService;

    @Override
    public String name() {
        return "Data Plane HTTP";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var executorService = Executors.newFixedThreadPool(10); // TODO make configurable

        var monitor = context.getMonitor();

        @SuppressWarnings("unchecked") var sourceFactory = new HttpDataSourceFactory(httpClient, retryPolicy, monitor);
        pipelineService.registerFactory(sourceFactory);

        var sinkFactory = new HttpDataSinkFactory(httpClient, executorService, 5, monitor);
        pipelineService.registerFactory(sinkFactory);
    }
}
