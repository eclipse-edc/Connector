/*
 *  Copyright (c) 2022 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

package org.eclipse.dataspaceconnector.gcp.dataplane.gcs;

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.EdcSetting;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Extension;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.util.concurrent.Executors;

@Extension(value = DataPlaneGcsExtension.NAME)
public class DataPlaneGcsExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Google Cloud Storage";

    @Inject
    PipelineService pipelineService;

    @Inject
    private Vault vault;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return "Data Plane Google Storage";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var executorService = Executors.newFixedThreadPool(10);
        var monitor = context.getMonitor();

        var sourceFactory = new GcsDataSourceFactory(monitor);
        pipelineService.registerFactory(sourceFactory);

        var sinkFactory = new GcsDataSinkFactory(executorService, monitor, vault, typeManager);
        pipelineService.registerFactory(sinkFactory);
    }
}
