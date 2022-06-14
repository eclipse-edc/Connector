/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package com.siemens.mindsphere.dataspaceconnector.extensions.api;

import net.jodah.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataTransferExecutorServiceContainer;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class HttpFileTransferExtension implements ServiceExtension {

    @Inject
    private PipelineService pipelineService;

    @Inject
    private DataTransferExecutorServiceContainer executorContainer;

    @Inject
    private OkHttpClient httpClient;

    @Inject
    @SuppressWarnings("rawtypes")
    private RetryPolicy retryPolicy;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var sinkFactory = new FileTransferDataSinkFactory(monitor, executorContainer.getExecutorService(), 5);
        pipelineService.registerFactory(sinkFactory);

        //MUST BE 1 partition - we can handle only one part at a time
        var sinkFactoryHttp = new HttpDataSinkFactory(httpClient, executorContainer.getExecutorService(), 1, monitor);
        pipelineService.registerFactory(sinkFactoryHttp);

        context.getMonitor().info("Http File Transfer Extension initialized!");
    }

}
