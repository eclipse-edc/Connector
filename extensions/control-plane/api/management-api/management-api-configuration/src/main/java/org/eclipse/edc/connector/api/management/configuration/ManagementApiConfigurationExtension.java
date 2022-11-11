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

package org.eclipse.edc.connector.api.management.configuration;

import org.eclipse.edc.api.auth.spi.AuthenticationRequestFilter;
import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

import static java.lang.String.format;

/**
 * Tells all the Management API controllers under which context alias they need to register their resources: either `default` or `management`
 *
 */
@Provides(ManagementApiConfiguration.class)
@Extension(value = ManagementApiConfigurationExtension.NAME)
public class ManagementApiConfigurationExtension implements ServiceExtension {

    public static final String NAME = "Management API configuration";

    public static final String DEPRECATED_MANAGEMENT_SETTINGS_GROUP_SUFFIX = "data";
    public static final String DATA_MANAGEMENT_CONTEXT_ALIAS = "management";
    public static final int DEFAULT_DATA_MANAGEMENT_API_PORT = 8181;
    public static final String DEPRECATED_DEFAULT_DATA_MANAGEMENT_API_CONTEXT_PATH = "/api";
    public static final String DEFAULT_DATA_MANAGEMENT_API_CONTEXT_PATH = "/api/v1/management";

    public static final String WEB_SERVICE_NAME = "Management API";

    /**
     * This is used to permit a softer transition from the deprecated `web.http.data` config group to the current
     * `web.http.management`
     */
    @Deprecated(since = "milestone8")
    public static final WebServiceSettings DEPRECATED_SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey("web.http." + DEPRECATED_MANAGEMENT_SETTINGS_GROUP_SUFFIX)
            .contextAlias(DATA_MANAGEMENT_CONTEXT_ALIAS)
            .defaultPath(DEPRECATED_DEFAULT_DATA_MANAGEMENT_API_CONTEXT_PATH)
            .defaultPort(DEFAULT_DATA_MANAGEMENT_API_PORT)
            .useDefaultContext(true)
            .name(WEB_SERVICE_NAME)
            .build();

    public static final WebServiceSettings SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey("web.http." + DATA_MANAGEMENT_CONTEXT_ALIAS)
            .contextAlias(DATA_MANAGEMENT_CONTEXT_ALIAS)
            .defaultPath(DEFAULT_DATA_MANAGEMENT_API_CONTEXT_PATH)
            .defaultPort(DEFAULT_DATA_MANAGEMENT_API_PORT)
            .useDefaultContext(true)
            .name(WEB_SERVICE_NAME)
            .build();

    @Inject
    private WebService webService;

    @Inject
    private WebServer webServer;

    @Inject
    private AuthenticationService authenticationService;

    @Inject
    private WebServiceConfigurer configurator;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        WebServiceSettings settings;
        var config = context.getConfig();
        if (config.hasPath(DEPRECATED_SETTINGS.apiConfigKey()) && !config.hasPath(SETTINGS.apiConfigKey())) {
            settings = DEPRECATED_SETTINGS;
            context.getMonitor().warning(
                    format("Deprecated settings group %s is being used for Management API configuration, please switch to the new group %s",
                            DEPRECATED_SETTINGS.apiConfigKey(), SETTINGS.apiConfigKey()));
        } else {
            settings = SETTINGS;
        }

        var webServiceConfiguration = configurator.configure(context, webServer, settings);

        context.registerService(ManagementApiConfiguration.class, new ManagementApiConfiguration(webServiceConfiguration.getContextAlias()));
        webService.registerResource(webServiceConfiguration.getContextAlias(), new AuthenticationRequestFilter(authenticationService));
    }
}
