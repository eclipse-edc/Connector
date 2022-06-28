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
import org.eclipse.dataspaceconnector.dataplane.api.controller.DataPlaneControlApiController;
import org.eclipse.dataspaceconnector.dataplane.api.controller.DataPlanePublicApiController;
import org.eclipse.dataspaceconnector.dataplane.api.validation.TokenValidationClient;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.concurrent.Executors;

/**
 * This extension provides the Data Plane API:
 * - Control API: set of endpoints to trigger/monitor/cancel data transfers that should be accessible only from the Control Plane.
 * - Public API: generic endpoint open to other participants of the Dataspace and used to proxy a data request to the actual data source.
 */
public class DataPlaneApiExtension implements ServiceExtension {
    @EdcSetting
    private static final String CONTROL_PLANE_VALIDATION_ENDPOINT = "edc.dataplane.token.validation.endpoint";

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
        var monitor = context.getMonitor();
        var typeManager = context.getTypeManager();

        var validationEndpoint = context.getConfig().getString(CONTROL_PLANE_VALIDATION_ENDPOINT);

        var tokenValidationClient = new TokenValidationClient(httpClient, validationEndpoint, typeManager.getMapper(), monitor);

        var executorService = context.getService(ExecutorInstrumentation.class)
                .instrument(Executors.newSingleThreadExecutor(), DataPlanePublicApiController.class.getSimpleName());

        webService.registerResource(CONTROL, new DataPlaneControlApiController(dataPlaneManager));

        var publicApiController = new DataPlanePublicApiController(dataPlaneManager, tokenValidationClient, monitor, executorService);
        webService.registerResource(PUBLIC, publicApiController);
    }
}


