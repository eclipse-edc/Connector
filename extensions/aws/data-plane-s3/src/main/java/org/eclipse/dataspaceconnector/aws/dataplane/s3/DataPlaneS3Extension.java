/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */
package org.eclipse.dataspaceconnector.aws.dataplane.s3;

import org.eclipse.dataspaceconnector.aws.s3.core.S3ClientProvider;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import java.util.concurrent.Executors;

public class DataPlaneS3Extension implements ServiceExtension {

    @Inject
    PipelineService pipelineService;

    @Inject
    S3ClientProvider s3ClientProvider;

    @Override
    public String name() {
        return "Data Plane Azure Storage";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var executorService = Executors.newFixedThreadPool(10); // TODO make configurable

        var monitor = context.getMonitor();
        var credentialsProvider = DefaultCredentialsProvider.create();

        var sourceFactory = new S3DataSourceFactory(s3ClientProvider, credentialsProvider);
        pipelineService.registerFactory(sourceFactory);

        var sinkFactory = new S3DataSinkFactory(s3ClientProvider, executorService, monitor, credentialsProvider);
        pipelineService.registerFactory(sinkFactory);
    }
}
