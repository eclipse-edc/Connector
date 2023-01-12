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

package org.eclipse.edc.connector.dataplane.gcp.storage;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataTransferExecutorServiceContainer;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.gcp.common.GcpCredentials;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

@Extension(value = DataPlaneGcsExtension.NAME)
public class DataPlaneGcsExtension implements ServiceExtension {
    public static final String NAME = "Data Plane Google Cloud Storage";

    @Inject
    PipelineService pipelineService;

    @Inject
    private Vault vault;

    @Inject
    private TypeManager typeManager;

    @Inject
    private DataTransferExecutorServiceContainer executorContainer;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        var gcpCredential = new GcpCredentials(vault, typeManager, monitor);


        var sourceFactory = new GcsDataSourceFactory(monitor, gcpCredential);
        pipelineService.registerFactory(sourceFactory);

        var sinkFactory = new GcsDataSinkFactory(executorContainer.getExecutorService(), monitor, gcpCredential);
        pipelineService.registerFactory(sinkFactory);
    }
}
