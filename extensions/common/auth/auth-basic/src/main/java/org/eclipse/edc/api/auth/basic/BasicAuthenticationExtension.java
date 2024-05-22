/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.edc.api.auth.basic;

import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static java.lang.String.format;

/**
 * Extension that registers an AuthenticationService that uses API Keys
 *
 * @deprecated this module is not supported anymore and it will be removed in the next iterations.
 */
@Provides(AuthenticationService.class)
@Extension(value = "Basic authentication")
@Deprecated(since = "0.6.5")
public class BasicAuthenticationExtension implements ServiceExtension {

    @Setting(value = "Key-value object defining authentication credentials stored in the vault", type = "map", required = true)
    static final String BASIC_AUTH = "edc.api.auth.basic.vault-keys";
    @Inject
    private Vault vault;
    @Inject
    private ApiAuthenticationRegistry authenticationRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        monitor.warning("The 'auth-basic' module has been deprecated and it will removed in the next iterations.");

        var credentials = context.getConfig(BASIC_AUTH)
                .getRelativeEntries().entrySet().stream()
                .map(entry -> new ConfigCredentials(entry.getKey(), entry.getValue()))
                .toList();

        if (!credentials.isEmpty()) {
            var authService = new BasicAuthenticationService(vault, credentials, monitor);
            authenticationRegistry.register("management-api", authService);
            monitor.info(format("API Authentication: basic auth configured with %s credential(s)", credentials.size()));
        } else {
            monitor.warning("API Authentication: no basic auth credentials provided");
        }
    }

}
