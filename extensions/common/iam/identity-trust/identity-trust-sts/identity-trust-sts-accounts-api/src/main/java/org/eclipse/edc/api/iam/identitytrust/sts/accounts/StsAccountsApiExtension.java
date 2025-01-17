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
import org.eclipse.edc.api.iam.identitytrust.sts.accounts.controller.StsAccountsApiController;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

@Extension(value = StsAccountsApiExtension.NAME, categories = { "sts", "dcp", "api" })
public class StsAccountsApiExtension implements ServiceExtension {

    public static final String NAME = "Secure Token Service Accounts API Extension";
    public static final String STS_ACCOUNTS_API_CONTEXT = "sts-accounts-api";
    @Deprecated(since = "0.12.0", forRemoval = true)
    private static final String EDC_API_ACCOUNTS_KEY = "edc.api.accounts.key";
    @Deprecated(since = "0.12.0", forRemoval = true)
    @Setting(description = "API key (or Vault alias) for the STS Accounts API's default authentication mechanism (token-based).", key = EDC_API_ACCOUNTS_KEY, required = false)
    private String accountsApiKeyOrAlias;

    @Inject
    private StsAccountService clientService;
    @Inject
    private WebService webService;
    @Inject
    private ApiAuthenticationRegistry authenticationRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (accountsApiKeyOrAlias != null) {
            var message = "Settings %s has been deprecated".formatted(EDC_API_ACCOUNTS_KEY) +
                    ", to configure authentication for sts-accounts api please configure it properly through the " +
                    "`web.http.sts-accounts.auth.<type>.<params>` settings, refer to the documentation for details.";
            context.getMonitor().severe(message);
            throw new EdcException(message);
        }

        var authenticationFilter = new AuthenticationRequestFilter(authenticationRegistry, STS_ACCOUNTS_API_CONTEXT);

        webService.registerResource(ApiContext.STS_ACCOUNTS, new StsAccountsApiController(clientService));
        webService.registerResource(ApiContext.STS_ACCOUNTS, authenticationFilter);
    }
}
