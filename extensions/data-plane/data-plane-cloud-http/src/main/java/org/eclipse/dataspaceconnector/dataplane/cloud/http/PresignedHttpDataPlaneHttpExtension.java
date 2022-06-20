/*
 *  Copyright (c) 2021 Microsoft Corporation
 *  Copyright (c) 2022 Siemens AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Siemens AG - changes to make it compatible with AWS S3 presigned URL for upload
 *
 */

package org.eclipse.dataspaceconnector.dataplane.cloud.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.dataplane.cloud.http.pipeline.PresignedHttpDataSinkFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataTransferExecutorServiceContainer;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * Provides support for reading data from an HTTP endpoint and sending data to an HTTP endpoint.
 */
public class PresignedHttpDataPlaneHttpExtension implements ServiceExtension {

    @Inject
    private OkHttpClient httpClient;

    @Inject
    @SuppressWarnings("rawtypes")
    private RetryPolicy retryPolicy;

    @Inject
    private PipelineService pipelineService;

    @Inject
    private DataTransferExecutorServiceContainer executorContainer;

    @Inject
    private Vault vault;

    @Override
    public String name() {
        return "Data Plane HTTP";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var sinkFactory = new PresignedHttpDataSinkFactory(httpClient, new ObjectMapper(), executorContainer.getExecutorService(),  monitor);
        pipelineService.registerFactory(sinkFactory);
    }
}
