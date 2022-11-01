/*
 *  Copyright (c) 2020, 2022 Microsoft Corporation
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
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
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

    public static final String CONTROL_PLANE_API_CONFIG = "web.http.controlplane";
    public static final String CONTROL_PLANE_CONTEXT_ALIAS = "controlplane";

    public static final int DEFAULT_CONTROL_PLANE_API_PORT = 8384;
    public static final String DEFAULT_CONTROL_PLANE_API_CONTEXT_PATH = "/api/v1/controlplane";

    private int port = DEFAULT_CONTROL_PLANE_API_PORT;
    private String path = DEFAULT_CONTROL_PLANE_API_CONTEXT_PATH;

    @Inject
    private WebServer webServer;
    @Inject
    private WebService webService;
    @Inject
    private TransferProcessManager transferProcessManager;
    @Inject
    private Hostname hostname;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        var alias = CONTROL_PLANE_CONTEXT_ALIAS;

        var config = context.getConfig(CONTROL_PLANE_API_CONFIG);
        if (config.getEntries().isEmpty()) {
            monitor.warning(format("Settings for [%s] and/or [%s] were not provided. Using default" +
                    " value(s) instead.", CONTROL_PLANE_API_CONFIG + ".path", CONTROL_PLANE_API_CONFIG + ".path"));
            webServer.addPortMapping(alias, port, path);
        } else {
            path = config.getString("path", path);
            port = config.getInteger("port", port);
        }

        context.getTypeManager().registerTypes(TransferProcessFailStateDto.class);

        monitor.info(format("Control Plane API will be available at [path=%s], [port=%s].", path, port));

        webService.registerResource(alias, new TransferProcessControlApiController(transferProcessManager));
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
        return String.format("http://%s:%s%s", hostname.get(), port, path);
    }


}
