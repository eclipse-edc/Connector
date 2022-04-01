/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.azure.dataplane.azurestorage;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.adapter.BlobAdapterFactory;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageDataSinkFactory;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageDataSourceFactory;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.concurrent.Executors;

/**
 * Provides support for reading data from an Azure Storage Blob endpoint and sending data to an Azure Storage Blob endpoint.
 */
public class DataPlaneAzureStorageExtension implements ServiceExtension {

    @Inject
    private RetryPolicy retryPolicy;

    @Inject
    private PipelineService pipelineService;

    @EdcSetting
    public static final String EDC_BLOBSTORE_ENDPOINT = "edc.blobstore.endpoint";

    @Override
    public String name() {
        return "Data Plane Azure Storage";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var blobstoreEndpoint = context.getSetting(EDC_BLOBSTORE_ENDPOINT, null);

        var executorService = Executors.newFixedThreadPool(10); // TODO make configurable

        var monitor = context.getMonitor();

        var blobAdapterFactory = new BlobAdapterFactory(blobstoreEndpoint);

        var sourceFactory = new AzureStorageDataSourceFactory(blobAdapterFactory, retryPolicy, monitor);
        pipelineService.registerFactory(sourceFactory);

        var sinkFactory = new AzureStorageDataSinkFactory(blobAdapterFactory, executorService, 5, monitor);
        pipelineService.registerFactory(sinkFactory);
    }
}
