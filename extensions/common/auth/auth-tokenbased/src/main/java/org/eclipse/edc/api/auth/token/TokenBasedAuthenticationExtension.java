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

import org.eclipse.edc.api.auth.spi.ApiAuthenticationProvider;
import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationProviderRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;

import java.util.Optional;

/**
 * Extension that registers an AuthenticationService that uses API Keys and register
 * an {@link ApiAuthenticationProvider} under the type called tokenbased
 */
@Extension(value = TokenBasedAuthenticationExtension.NAME)
public class TokenBasedAuthenticationExtension implements ServiceExtension {

    public static final String NAME = "Static token API Authentication";
    private static final String AUTH_KEY = "auth";
    private static final String CONFIG_ALIAS = "web.http.<context>." + AUTH_KEY + ".";
    private static final String TOKENBASED_TYPE = "tokenbased";
    @Deprecated(since = "0.12.0", forRemoval = true)
    private static final String AUTH_SETTING_APIKEY = "edc.api.auth.key";
    @Deprecated(since = "0.12.0", forRemoval = true)
    private static final String AUTH_SETTING_APIKEY_ALIAS = "edc.api.auth.key.alias";

    @Setting(context = CONFIG_ALIAS, description = "The api key to use for the <context>")
    public static final String AUTH_API_KEY = "key";
    @Setting(context = CONFIG_ALIAS, description = "The vault api key alias to use for the <context>")
    public static final String AUTH_API_KEY_ALIAS = "key.alias";
    @Setting(description = "DEPRECATED: auth key", key = AUTH_SETTING_APIKEY, required = false)
    @Deprecated(since = "0.12.0", forRemoval = true)
    private String deprecatedApiKey;
    @Setting(description = "DEPRECATED: auth key alias", key = AUTH_SETTING_APIKEY_ALIAS, required = false)
    @Deprecated(since = "0.12.0", forRemoval = true)
    private String deprecatedApiKeyAlias;

    @Inject
    private Vault vault;
    @Inject
    private ApiAuthenticationProviderRegistry providerRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (deprecatedApiKey != null || deprecatedApiKeyAlias != null) {
            var message = "Settings %s and %s have been removed".formatted(AUTH_SETTING_APIKEY, AUTH_SETTING_APIKEY_ALIAS) +
                    ", to configure token based authentication for management api please configure it properly through the " +
                    "`web.http.management.auth.%s` or `web.http.management.auth.%s` settings".formatted(AUTH_API_KEY, AUTH_API_KEY_ALIAS);
            context.getMonitor().severe(message);
            throw new EdcException(message);
        }

        providerRegistry.register(TOKENBASED_TYPE, this::tokenBasedProvider);
    }

    public Result<AuthenticationService> tokenBasedProvider(Config config) {
        var apiKey = Optional.ofNullable(config.getString(AUTH_API_KEY_ALIAS, null))
                .map(alias -> vault.resolveSecret(alias))
                .orElseGet(() -> config.getString(AUTH_API_KEY));

        return Result.success(new TokenBasedAuthenticationService(apiKey));
    }
}
