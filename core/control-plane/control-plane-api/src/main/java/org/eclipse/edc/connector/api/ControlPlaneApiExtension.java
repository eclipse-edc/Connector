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

import org.eclipse.edc.connector.api.control.configuration.ControlApiConfiguration;
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

import static java.lang.String.format;

/**
 * {@link ControlPlaneApiExtension } exposes HTTP endpoints for internal interaction with the Control Plane
 */
@Extension(value = ControlPlaneApiExtension.NAME)
public class ControlPlaneApiExtension implements ServiceExtension {

    public static final String NAME = "Control Plane API";

    private static final String DEPRECATED_CONTROLPANE_CONFIG_GROUP = "web.http.controlplane";
    private static final String CONTROL_PLANE_CONTEXT_ALIAS = "controlplane";
    private static final int DEFAULT_CONTROL_PLANE_API_PORT = 8384;
    private static final String DEFAULT_CONTROL_PLANE_API_CONTEXT_PATH = "/api/v1/controlplane";

    /**
     * This deprecation is used to permit a softer transition from the deprecated `web.http.controlpane` config group to
     * the current `web.http.control`
     *
     * @deprecated "web.http.control" config should be used instead of "web.http.controlplane"
     */
    @Deprecated(since = "milestone8")
    public static final WebServiceSettings DEPRECATED_SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey(DEPRECATED_CONTROLPANE_CONFIG_GROUP)
            .contextAlias(CONTROL_PLANE_CONTEXT_ALIAS)
            .defaultPath(DEFAULT_CONTROL_PLANE_API_CONTEXT_PATH)
            .defaultPort(DEFAULT_CONTROL_PLANE_API_PORT)
            .name(NAME)
            .build();

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

    @Inject
    private ControlApiConfiguration controlApiConfiguration;

    private WebServiceConfiguration webServiceConfiguration;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (context.getConfig().hasPath(DEPRECATED_CONTROLPANE_CONFIG_GROUP)) {
            webServiceConfiguration = configurator.configure(context, webServer, DEPRECATED_SETTINGS);
            context.getMonitor().warning(
                    format("Deprecated settings group %s is being used for Control API configuration, please switch to the new group %s",
                            DEPRECATED_SETTINGS.apiConfigKey(), "web.http." + controlApiConfiguration.getContextAlias()));
        } else {
            webServiceConfiguration = controlApiConfiguration;
        }

        context.getTypeManager().registerTypes(TransferProcessFailStateDto.class);

        webService.registerResource(webServiceConfiguration.getContextAlias(), new TransferProcessControlApiController(transferProcessService));
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
        return String.format("http://%s:%s%s", hostname.get(), webServiceConfiguration.getPort(), webServiceConfiguration.getPath());
    }

}
