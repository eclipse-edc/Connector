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
 *       Mercedes-Benz Tech Innovation GmbH - publish public api context into dedicated swagger hub page
 *
 */

package org.eclipse.edc.connector.dataplane.api;

import org.eclipse.edc.connector.dataplane.api.controller.DataPlanePublicApiController;
import org.eclipse.edc.connector.dataplane.api.validation.ConsumerPullTransferDataAddressResolver;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

import java.util.concurrent.Executors;

/**
 * This extension provides generic endpoints which are open to public participants of the Dataspace to execute
 * requests on the actual data source.
 */
@Extension(value = DataPlanePublicApiExtension.NAME)
public class DataPlanePublicApiExtension implements ServiceExtension {
    public static final String NAME = "Data Plane Public API";
    private static final int DEFAULT_PUBLIC_PORT = 8185;
    private static final String PUBLIC_API_CONFIG = "web.http.public";
    private static final String PUBLIC_CONTEXT_ALIAS = "public";
    private static final String PUBLIC_CONTEXT_PATH = "/api/v1/public";

    @Setting
    private static final String CONTROL_PLANE_VALIDATION_ENDPOINT = "edc.dataplane.token.validation.endpoint";

    private static final int DEFAULT_THREAD_POOL = 10;

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
    private PipelineService pipelineService;

    @Inject
    private WebService webService;

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private TypeManager typeManager;

    @Inject
    private ExecutorInstrumentation executorInstrumentation;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var validationEndpoint = context.getConfig().getString(CONTROL_PLANE_VALIDATION_ENDPOINT);
        var dataAddressResolver = new ConsumerPullTransferDataAddressResolver(httpClient, validationEndpoint, typeManager.getMapper());
        var configuration = webServiceConfigurer.configure(context, webServer, PUBLIC_SETTINGS);
        var executorService = executorInstrumentation.instrument(
                Executors.newFixedThreadPool(DEFAULT_THREAD_POOL),
                "Data plane proxy transfers"
        );
        var monitor = context.getMonitor().withPrefix("DataPlane Public API");
        var publicApiController = new DataPlanePublicApiController(pipelineService, dataAddressResolver, executorService, monitor);
        webService.registerResource(configuration.getContextAlias(), publicApiController);

        monitor.warning("This public API controller is scheduled for removal. Please consider upgrading your deployment " +
                "to the data-plane-public-api-v2 module. The Data Plane Public API will then be available under at /v2/ prefix.");
    }
}
