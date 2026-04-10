/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.api.auth.configuration;

import org.eclipse.edc.api.auth.spi.AuthenticationRequestFilter;
import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationProviderRegistry;
import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.web.spi.WebService;

import java.util.Map;
import java.util.Optional;

import static org.eclipse.edc.api.auth.configuration.ApiAuthenticationConfigurationExtension.NAME;

@Extension(NAME)
public class ApiAuthenticationConfigurationExtension implements ServiceExtension {

    public static final String NAME = "Api Authentication Configuration Extension";

    @SettingContext("web.http")
    @Configuration
    private Map<String, AuthenticationConfiguration> authenticationConfigurationMap;

    @Inject
    private ApiAuthenticationProviderRegistry providerRegistry;

    @Inject
    private ApiAuthenticationRegistry authenticationRegistry;

    @Inject
    private WebService webService;

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void prepare() {
        authenticationConfigurationMap.entrySet().stream()
                .filter(entry -> entry.getValue().type() != null)
                .forEach(entry -> {
                    var serviceResult = configureService(entry.getValue());
                    if (serviceResult.failed()) {
                        throw new EdcException("Failed to configure authentication for context %s: %s".formatted(entry.getKey(), serviceResult.getFailureDetail()));
                    }
                    authenticationRegistry.register(entry.getKey(), serviceResult.getContent());
                    var authenticationFilter = new AuthenticationRequestFilter(authenticationRegistry, entry.getKey());
                    webService.registerResource(entry.getKey(), authenticationFilter);
                    monitor.debug("Configured %s authentication for context %s".formatted(entry.getValue().type(), entry.getKey()));
                });
    }

    private Result<AuthenticationService> configureService(AuthenticationConfiguration configuration) {
        var type = configuration.type();
        return Optional.ofNullable(providerRegistry.resolve(type))
                .map(provider -> provider.provide(configuration.config()))
                .orElseGet(() -> Result.failure("Authentication provider for type %s not found".formatted(type)));
    }

    @Settings
    private record AuthenticationConfiguration(
            @Setting(key = "auth.type", required = false) String type,
            @Setting(key = "auth") Config config) {
    }
}
