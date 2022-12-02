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

package org.eclipse.edc.connector.dataplane.api;

import org.eclipse.edc.connector.api.control.configuration.ControlApiConfiguration;
import org.eclipse.edc.connector.dataplane.api.controller.DataPlaneControlApiController;
import org.eclipse.edc.connector.dataplane.api.controller.DataPlanePublicApiController;
import org.eclipse.edc.connector.dataplane.api.validation.ConsumerPullTransferDataAddressResolver;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

import java.util.concurrent.Executors;

/**
 * This extension provides the Data Plane API:
 * - Control API: set of endpoints to trigger/monitor/cancel data transfers that should be accessible only from the Control Plane.
 * - Public API: generic endpoint open to other participants of the Dataspace and used to proxy a data request to the actual data source.
 */
@Extension(value = DataPlaneApiExtension.NAME)
public class DataPlaneApiExtension implements ServiceExtension {
    public static final String NAME = "Data Plane API";
    private static final int DEFAULT_PUBLIC_PORT = 8185;
    private static final String PUBLIC_API_CONFIG = "web.http.public";
    private static final String PUBLIC_CONTEXT_ALIAS = "public";
    private static final String PUBLIC_CONTEXT_PATH = "/api/v1/public";

    @Setting
    private static final String CONTROL_PLANE_VALIDATION_ENDPOINT = "edc.dataplane.token.validation.endpoint";

    private static final WebServiceSettings PUBLIC_SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey(PUBLIC_API_CONFIG)
            .contextAlias(PUBLIC_CONTEXT_ALIAS)
            .defaultPath(PUBLIC_CONTEXT_PATH)
            .defaultPort(DEFAULT_PUBLIC_PORT)
            .name(NAME)
            .build();

    @Inject
    private WebServer webServer;

    @Inject
    private WebServiceConfigurer webServiceConfigurer;

    @Inject
    private DataPlaneManager dataPlaneManager;

    @Inject
    private WebService webService;

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private ControlApiConfiguration controlApiConfiguration;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        var typeManager = context.getTypeManager();

        var validationEndpoint = context.getConfig().getString(CONTROL_PLANE_VALIDATION_ENDPOINT);

        var dataAddressResolver = new ConsumerPullTransferDataAddressResolver(httpClient, validationEndpoint, typeManager.getMapper());

        var executorService = context.getService(ExecutorInstrumentation.class)
                .instrument(Executors.newSingleThreadExecutor(), DataPlanePublicApiController.class.getSimpleName());

        webService.registerResource(controlApiConfiguration.getContextAlias(), new DataPlaneControlApiController(dataPlaneManager));

        var configuration = webServiceConfigurer.configure(context, webServer, PUBLIC_SETTINGS);
        var publicApiController = new DataPlanePublicApiController(dataPlaneManager, dataAddressResolver, monitor, executorService);
        webService.registerResource(configuration.getContextAlias(), publicApiController);
    }
}
