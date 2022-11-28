/*
 *  Copyright (c) 2020 - 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.control.configuration;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

/**
 * Tells all the Control API controllers under which context alias they need to register their resources: either
 * `default` or `control`
 */
@Provides(ControlApiConfiguration.class)
@Extension(value = ControlApiConfigurationExtension.NAME)
public class ControlApiConfigurationExtension implements ServiceExtension {

    public static final String NAME = "Control API configuration";

    private static final String WEB_SERVICE_NAME = "Control API";
    private static final String CONTROL_CONTEXT_ALIAS = "control";
    private static final int DEFAULT_CONTROL_API_PORT = 9191;
    private static final String DEFAULT_CONTROL_API_CONTEXT_PATH = "/api/v1/control";

    @Inject
    private WebServer webServer;

    @Inject
    private WebServiceConfigurer configurator;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public ControlApiConfiguration controlApiConfiguration(ServiceExtensionContext context) {
        var settings = WebServiceSettings.Builder.newInstance()
                .apiConfigKey("web.http." + CONTROL_CONTEXT_ALIAS)
                .contextAlias(CONTROL_CONTEXT_ALIAS)
                .defaultPath(DEFAULT_CONTROL_API_CONTEXT_PATH)
                .defaultPort(DEFAULT_CONTROL_API_PORT)
                .useDefaultContext(true)
                .name(WEB_SERVICE_NAME)
                .build();

        var webServiceConfiguration = configurator.configure(context, webServer, settings);

        return new ControlApiConfiguration(webServiceConfiguration);
    }
}
