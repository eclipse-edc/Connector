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
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Extension that registers an AuthenticationService that uses API Keys
 */
@Provides(AuthenticationService.class)
@Extension(value = "Basic authentication")
public class BasicAuthenticationExtension implements ServiceExtension {

    @Setting(value = "The vault keys to use for basic authentication in the API")
    public static final String BASIC_AUTH = "edc.api.auth.basic.vault-keys";
    @Inject
    private Vault vault;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var credentials = context.getConfig(BASIC_AUTH)
                .getRelativeEntries().entrySet().stream()
                .map(entry -> new ConfigCredentials(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        // Register basic authentication filter
        if (!credentials.isEmpty()) {
            var authService = new BasicAuthenticationService(vault, credentials, monitor);
            context.registerService(AuthenticationService.class, authService);
            monitor.info(format("API Authentication: basic auth configured with %s credential(s)", credentials.size()));
        } else {
            monitor.warning("API Authentication: no basic auth credentials provided");
        }
    }

    static class ConfigCredentials {
        private final String username;
        private final String vaultKey;

        ConfigCredentials(String username, String vaultKey) {
            this.username = username;
            this.vaultKey = vaultKey;
        }

        public String getUsername() {
            return username;
        }

        public String getVaultKey() {
            return vaultKey;
        }
    }
}
