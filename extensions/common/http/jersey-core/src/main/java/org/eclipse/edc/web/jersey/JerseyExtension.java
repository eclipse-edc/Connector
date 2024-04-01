/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.web.jersey;

import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.jersey.validation.ResourceInterceptorBinder;
import org.eclipse.edc.web.jersey.validation.ResourceInterceptorProvider;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.validation.InterceptorFunctionRegistry;

@Provides(WebService.class)
public class JerseyExtension implements ServiceExtension {
    private JerseyRestService jerseyRestService;

    @Setting
    public static final String CORS_CONFIG_ORIGINS_SETTING = "edc.web.rest.cors.origins";
    @Setting
    public static final String CORS_CONFIG_ENABLED_SETTING = "edc.web.rest.cors.enabled";
    @Setting
    public static final String CORS_CONFIG_HEADERS_SETTING = "edc.web.rest.cors.headers";
    @Setting
    public static final String CORS_CONFIG_METHODS_SETTING = "edc.web.rest.cors.methods";

    @Inject
    private WebServer webServer;

    @Inject
    private TypeManager typeManager;

    private ResourceInterceptorProvider provider;

    @Override
    public String name() {
        return "Jersey Web Service";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var configuration = JerseyConfiguration.Builder.newInstance()
                .allowedHeaders(context.getSetting(CORS_CONFIG_HEADERS_SETTING, "origin, content-type, accept, authorization"))
                .allowedOrigins(context.getSetting(CORS_CONFIG_ORIGINS_SETTING, "*"))
                .allowedMethods(context.getSetting(CORS_CONFIG_METHODS_SETTING, "GET, POST, DELETE, PUT, OPTIONS"))
                .corsEnabled(context.getSetting(CORS_CONFIG_ENABLED_SETTING, false))
                .build();

        jerseyRestService = new JerseyRestService(webServer, typeManager, configuration, monitor);

        provider = new ResourceInterceptorProvider();
        jerseyRestService.registerInstance(() -> new ResourceInterceptorBinder(provider));
        context.registerService(WebService.class, jerseyRestService);

    }

    @Override
    public void start() {
        jerseyRestService.start();
    }

    @Provider
    public InterceptorFunctionRegistry createInterceptorFunctionRegistry() {
        return provider;
    }
}
