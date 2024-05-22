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
 *       Mercedes-Benz Tech Innovation GmbH - add README.md; authentication key can be retrieved from vault
 *       Fraunhofer Institute for Software and Systems Engineering - update monitor info
 *
 */

package org.eclipse.edc.api.auth.token;

import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.Optional;
import java.util.UUID;

/**
 * Extension that registers an AuthenticationService that uses API Keys
 */
@Provides(AuthenticationService.class)
@Extension(value = TokenBasedAuthenticationExtension.NAME)
public class TokenBasedAuthenticationExtension implements ServiceExtension {

    public static final String NAME = "Static token API Authentication";
    @Setting
    private static final String AUTH_SETTING_APIKEY = "edc.api.auth.key";
    @Setting
    private static final String AUTH_SETTING_APIKEY_ALIAS = "edc.api.auth.key.alias";

    @Inject
    private Vault vault;
    @Inject
    private ApiAuthenticationRegistry authenticationRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var apiKey = Optional.ofNullable(context.getSetting(AUTH_SETTING_APIKEY_ALIAS, null))
                .map(alias -> vault.resolveSecret(alias))
                .orElseGet(() -> context.getSetting(AUTH_SETTING_APIKEY, UUID.randomUUID().toString()));

        authenticationRegistry.register("management-api", new TokenBasedAuthenticationService(apiKey));
    }
}
