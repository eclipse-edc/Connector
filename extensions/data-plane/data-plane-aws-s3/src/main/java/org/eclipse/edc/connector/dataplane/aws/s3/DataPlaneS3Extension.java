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

package org.eclipse.edc.connector.dataplane.aws.s3;

import org.eclipse.edc.aws.s3.AwsClientProvider;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.concurrent.Executors;

@Extension(value = DataPlaneS3Extension.NAME)
public class DataPlaneS3Extension implements ServiceExtension {

    public static final String NAME = "Data Plane S3 Storage";
    @Inject
    private PipelineService pipelineService;

    @Inject
    private AwsClientProvider awsClientProvider;

    @Inject
    private Vault vault;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var executorService = Executors.newFixedThreadPool(10); // TODO make configurable

        var monitor = context.getMonitor();

        var sourceFactory = new S3DataSourceFactory(awsClientProvider, vault, typeManager);
        pipelineService.registerFactory(sourceFactory);

        var sinkFactory = new S3DataSinkFactory(awsClientProvider, executorService, monitor, vault, typeManager);
        pipelineService.registerFactory(sinkFactory);
    }
}
