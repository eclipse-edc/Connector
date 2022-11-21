/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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

package org.eclipse.edc.connector.api;

import org.eclipse.edc.connector.api.transferprocess.TransferProcessControlApiController;
import org.eclipse.edc.connector.api.transferprocess.model.TransferProcessFailStateDto;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.callback.ControlPlaneApiUrl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * {@link ControlPlaneApiExtension } exposes HTTP endpoints for internal interaction with the Control Plane
 */
@Extension(value = ControlPlaneApiExtension.NAME)
public class ControlPlaneApiExtension implements ServiceExtension {

    public static final String NAME = "Control Plane API";

    public static final String CONTROL_PLANE_API_CONFIG = "web.http.controlplane";
    public static final String CONTROL_PLANE_CONTEXT_ALIAS = "controlplane";
    public static final int DEFAULT_CONTROL_PLANE_API_PORT = 8384;
    public static final String DEFAULT_CONTROL_PLANE_API_CONTEXT_PATH = "/api/v1/controlplane";

    @Inject
    private WebServer webServer;
    @Inject
    private WebService webService;

    @Inject
    private Hostname hostname;

    @Inject
    private WebServiceConfigurer configurator;

    @Inject
    private TransferProcessService transferProcessService;

    private WebServiceConfiguration configuration;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var settings = WebServiceSettings.Builder.newInstance()
                .apiConfigKey(CONTROL_PLANE_API_CONFIG)
                .contextAlias(CONTROL_PLANE_CONTEXT_ALIAS)
                .defaultPath(DEFAULT_CONTROL_PLANE_API_CONTEXT_PATH)
                .defaultPort(DEFAULT_CONTROL_PLANE_API_PORT)
                .name(NAME)
                .build();

        configuration = configurator.configure(context, webServer, settings);
        context.getTypeManager().registerTypes(TransferProcessFailStateDto.class);

        webService.registerResource(configuration.getContextAlias(), new TransferProcessControlApiController(transferProcessService));
    }


    @Provider
    public ControlPlaneApiUrl controlPlaneApiUrl(ServiceExtensionContext context) {
        var s = getApiUrl();
        try {
            var url = new URL(s);
            return () -> url;
        } catch (MalformedURLException e) {
            context.getMonitor().severe("Error creating callback endpoint", e);
            throw new EdcException(e);
        }
    }

    @NotNull
    private String getApiUrl() {
        return String.format("http://%s:%s%s", hostname.get(), configuration.getPort(), configuration.getPath());
    }


}
