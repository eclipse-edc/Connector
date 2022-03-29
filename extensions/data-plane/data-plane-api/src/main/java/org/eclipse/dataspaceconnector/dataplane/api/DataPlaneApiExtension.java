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
package org.eclipse.dataspaceconnector.dataplane.api;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.dataplane.api.controller.ContainerRequestContextApiImpl;
import org.eclipse.dataspaceconnector.dataplane.api.controller.DataPlanePublicApiController;
import org.eclipse.dataspaceconnector.dataplane.api.controller.DataPlaneTransferController;
import org.eclipse.dataspaceconnector.dataplane.api.validation.RemoteTokenValidationService;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.concurrent.Executors;

/**
 * Provides the control plane and public APIs for a data plane server.
 */
public class DataPlaneApiExtension implements ServiceExtension {
    @EdcSetting
    private static final String CONTROL_PLANE_VALIDATION_ENDPOINT = "edc.controlplane.validation-endpoint";

    private static final String CONTROL = "control";
    private static final String PUBLIC = "public";

    @Inject
    private DataPlaneManager dataPlaneManager;

    @Inject
    private WebService webService;

    @Inject
    private OkHttpClient httpClient;

    @Override
    public String name() {
        return "Data Plane API";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var controlPlaneAddress = context.getSetting(CONTROL_PLANE_VALIDATION_ENDPOINT, "/api/validation");
        var tokenValidationClient = new RemoteTokenValidationService(httpClient, controlPlaneAddress, context.getTypeManager().getMapper());

        var executorService = context.getService(ExecutorInstrumentation.class)
                .instrument(Executors.newSingleThreadExecutor(), DataPlanePublicApiController.class.getSimpleName());

        webService.registerResource(CONTROL, new DataPlaneTransferController(dataPlaneManager));

        var publicApiController = new DataPlanePublicApiController(dataPlaneManager, tokenValidationClient, context.getMonitor(), new ContainerRequestContextApiImpl(), context.getTypeManager(), executorService);
        webService.registerResource(PUBLIC, publicApiController);
    }
}


