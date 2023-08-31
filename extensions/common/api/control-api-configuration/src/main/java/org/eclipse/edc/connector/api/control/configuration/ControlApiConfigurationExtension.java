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

import org.eclipse.edc.connector.transfer.spi.callback.ControlApiUrl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

import java.net.MalformedURLException;
import java.net.URL;

import static java.lang.String.format;

/**
 * Tells all the Control API controllers under which context alias they need to register their resources: either
 * `default` or `control`
 */
@Extension(value = ControlApiConfigurationExtension.NAME)
@Provides({ ControlApiConfiguration.class, ControlApiUrl.class })
public class ControlApiConfigurationExtension implements ServiceExtension {

    public static final String NAME = "Control API configuration";

    @Setting(value = "Configures endpoint for reaching the Control API. If it's missing it defaults to the hostname configuration.")
    public static final String CONTROL_API_ENDPOINT = "edc.control.endpoint";
    public static final String CONTROL_CONTEXT_ALIAS = "control";
    private static final String WEB_SERVICE_NAME = "Control API";
    private static final int DEFAULT_CONTROL_API_PORT = 9191;
    private static final String DEFAULT_CONTROL_API_CONTEXT_PATH = "/api/v1/control";
    public static final WebServiceSettings SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey("web.http." + CONTROL_CONTEXT_ALIAS)
            .contextAlias(CONTROL_CONTEXT_ALIAS)
            .defaultPath(DEFAULT_CONTROL_API_CONTEXT_PATH)
            .defaultPort(DEFAULT_CONTROL_API_PORT)
            .useDefaultContext(true)
            .name(WEB_SERVICE_NAME)
            .build();
    @Inject
    private WebServer webServer;
    @Inject
    private WebServiceConfigurer configurator;

    @Inject
    private Hostname hostname;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var config = configurator.configure(context, webServer, SETTINGS);
        var callbackAddress = controlPlaneApiUrl(context, config);

        context.registerService(ControlApiConfiguration.class, new ControlApiConfiguration(config));
        context.registerService(ControlApiUrl.class, callbackAddress);

    }

    private ControlApiUrl controlPlaneApiUrl(ServiceExtensionContext context, WebServiceConfiguration config) {
        var callbackAddress = context.getSetting(CONTROL_API_ENDPOINT, format("http://%s:%s%s", hostname.get(), config.getPort(), config.getPath()));
        try {
            var url = new URL(callbackAddress);
            return () -> url;
        } catch (MalformedURLException e) {
            context.getMonitor().severe("Error creating control plane endpoint url", e);
            throw new EdcException(e);
        }
    }
}
