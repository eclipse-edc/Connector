/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.api.datamanagement.configuration;

import org.eclipse.dataspaceconnector.api.auth.AuthenticationRequestFilter;
import org.eclipse.dataspaceconnector.api.auth.AuthenticationService;
import org.eclipse.dataspaceconnector.api.exception.mappers.EdcApiExceptionMapper;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

@Provides(DataManagementApiConfiguration.class)
public class DataManagementApiConfigurationExtension implements ServiceExtension {

    public static final String WEB_DATA_CONFIG = "web.http.data";
    private static final String DEFAULT_DATAMANAGEMENT_ALIAS = "default";

    @Inject
    private WebService webService;

    @Inject(required = false)
    private AuthenticationService service;

    @Override
    public String name() {
        return "Data Management API configuration";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        var contextAlias = DEFAULT_DATAMANAGEMENT_ALIAS;

        // if no dedicated web.http.data.[port|path] config exists, we'll expose it under the default mapping
        var monitor = context.getMonitor();
        var port = 8181;
        var path = "/api";
        var config = context.getConfig(WEB_DATA_CONFIG);
        if (config.getEntries().isEmpty()) {
            monitor.warning("No [web.http.data.port] or [web.http.data.path] configuration has been provided, therefor the default will be used. " +
                    "This means that the AuthenticationRequestFilter and the EdcApiExceptionMapper " + "will also be registered for the default context and fire for EVERY request on that context!");
        } else {
            contextAlias = "data";
            port = config.getInteger("port", port);
            path = config.getString("path", path);

        }

        monitor.info(format("The DataManagementApi will be available under port=%s, path=%s", port, path));

        // the DataManagementApiConfiguration tells all DataManagementApi controllers under which context alias
        // they need to register their resources: either `default` or `data`
        context.registerService(DataManagementApiConfiguration.class, new DataManagementApiConfiguration(contextAlias));
        var srv = ofNullable(service).orElse(headers -> true);
        webService.registerResource(contextAlias, new AuthenticationRequestFilter(srv));
        webService.registerResource(contextAlias, new EdcApiExceptionMapper());
    }
}
