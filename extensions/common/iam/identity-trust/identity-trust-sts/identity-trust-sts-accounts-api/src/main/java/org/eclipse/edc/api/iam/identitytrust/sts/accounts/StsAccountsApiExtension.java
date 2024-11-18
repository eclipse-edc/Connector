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

package org.eclipse.edc.api.iam.identitytrust.sts.accounts;

import org.eclipse.edc.api.auth.spi.AuthenticationRequestFilter;
import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationRegistry;
import org.eclipse.edc.api.auth.token.TokenBasedAuthenticationService;
import org.eclipse.edc.api.iam.identitytrust.sts.accounts.controller.StsAccountsApiController;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import static java.util.Optional.ofNullable;

@Extension(value = StsAccountsApiExtension.NAME, categories = { "sts", "dcp", "api" })
public class StsAccountsApiExtension implements ServiceExtension {

    public static final String NAME = "Secure Token Service Accounts API Extension";
    public static final String STS_ACCOUNTS_API_CONTEXT = "sts-accounts-api";

    @Setting(description = "API key (or Vault alias) for the STS Accounts API's default authentication mechanism (token-based).", key = "edc.api.accounts.key")
    private String accountsApiKeyOrAlias;

    @Inject
    private StsAccountService clientService;

    @Inject
    private WebService webService;
    @Inject
    private ApiAuthenticationRegistry authenticationRegistry;

    @Inject
    private Vault vault;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        if (!authenticationRegistry.hasService(STS_ACCOUNTS_API_CONTEXT)) {
            authenticationRegistry.register(STS_ACCOUNTS_API_CONTEXT, new TokenBasedAuthenticationService(resolveApiKey(context)));
        }
        var authenticationFilter = new AuthenticationRequestFilter(authenticationRegistry, STS_ACCOUNTS_API_CONTEXT);

        webService.registerResource(ApiContext.STS_ACCOUNTS, new StsAccountsApiController(clientService));
        webService.registerResource(ApiContext.STS_ACCOUNTS, authenticationFilter);
    }

    private String resolveApiKey(ServiceExtensionContext context) {
        return ofNullable(vault.resolveSecret(accountsApiKeyOrAlias))
                .orElse(accountsApiKeyOrAlias);
    }
}
