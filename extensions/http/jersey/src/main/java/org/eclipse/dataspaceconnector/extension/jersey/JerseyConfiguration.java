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

package org.eclipse.dataspaceconnector.extension.jersey;

import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * Jersey extension configuration class
 */
public class JerseyConfiguration {
    @EdcSetting
    public static final String ERROR_RESPONSE_VERBOSE_SETTING = "edc.web.rest.error.response.verbose";
    @EdcSetting
    public static final String CORS_CONFIG_ORIGINS_SETTING = "edc.web.rest.cors.origins";
    @EdcSetting
    public static final String CORS_CONFIG_ENABLED_SETTING = "edc.web.rest.cors.enabled";
    @EdcSetting
    public static final String CORS_CONFIG_HEADERS_SETTING = "edc.web.rest.cors.headers";
    @EdcSetting
    public static final String CORS_CONFIG_METHODS_SETTING = "edc.web.rest.cors.methods";
    private String allowedOrigins;
    private String allowedHeaders;
    private String allowedMethods;
    private boolean corsEnabled;
    private boolean errorResponseVerbose;

    private JerseyConfiguration() {
    }

    public static JerseyConfiguration from(ServiceExtensionContext context) {
        var origins = context.getSetting(CORS_CONFIG_ORIGINS_SETTING, "*");
        var headers = context.getSetting(CORS_CONFIG_HEADERS_SETTING, "origin, content-type, accept, authorization");
        var allowedMethods = context.getSetting(CORS_CONFIG_METHODS_SETTING, "GET, POST, DELETE, PUT, OPTIONS");
        var enabled = context.getSetting(CORS_CONFIG_ENABLED_SETTING, false);
        var config = new JerseyConfiguration();
        config.allowedHeaders = headers;
        config.allowedOrigins = origins;
        config.allowedMethods = allowedMethods;
        config.corsEnabled = enabled;
        config.errorResponseVerbose = context.getSetting(ERROR_RESPONSE_VERBOSE_SETTING, false);

        return config;
    }

    public static JerseyConfiguration none() {
        return new JerseyConfiguration();
    }

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public String getAllowedHeaders() {
        return allowedHeaders;
    }

    public String getAllowedMethods() {
        return allowedMethods;
    }

    public boolean isCorsEnabled() {
        return corsEnabled;
    }

    public boolean isErrorResponseVerbose() {
        return errorResponseVerbose;
    }
}
