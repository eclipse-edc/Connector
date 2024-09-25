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
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.api.iam.identitytrust.sts.accounts.StsAccountsApiExtension.STS_ACCOUNTS_API_CONTEXT;
import static org.eclipse.edc.api.iam.identitytrust.sts.accounts.StsAccountsApiExtension.STS_ACCOUNTS_API_KEY;
import static org.eclipse.edc.web.spi.configuration.ApiContext.STS_ACCOUNTS;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class StsAccountsApiExtensionTest {

    private final ApiAuthenticationRegistry apiAuthenticationRegistry = mock();
    private final WebService webService = mock();
    private final Vault vault = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(ApiAuthenticationRegistry.class, apiAuthenticationRegistry);
        context.registerService(WebService.class, webService);
        context.registerService(Vault.class, vault);
    }

    @Test
    void initialize_noAuthServicePresent_withApiKeyInVault(StsAccountsApiExtension extension, ServiceExtensionContext context) {
        var config = mock(Config.class);
        when(config.getString(eq(STS_ACCOUNTS_API_KEY)))
                .thenReturn("test-api-key");
        when(context.getConfig()).thenReturn(config);
        when(apiAuthenticationRegistry.hasService(eq(STS_ACCOUNTS_API_CONTEXT))).thenReturn(false);

        extension.initialize(context);

        verify(vault).resolveSecret(eq("test-api-key"));
        verify(apiAuthenticationRegistry).register(eq(STS_ACCOUNTS_API_CONTEXT), isA(TokenBasedAuthenticationService.class));
        verify(webService).registerResource(eq(STS_ACCOUNTS), isA(StsAccountsApiController.class));
        verify(webService).registerResource(eq(STS_ACCOUNTS), isA(AuthenticationRequestFilter.class));
    }

    @Test
    void initialize_noAuthServicePresent_withApiKeyInConfig(StsAccountsApiExtension extension, ServiceExtensionContext context) {
        var config = mock(Config.class);
        when(config.getString(eq(STS_ACCOUNTS_API_KEY)))
                .thenReturn("test-api-key");
        when(context.getConfig()).thenReturn(config);
        when(apiAuthenticationRegistry.hasService(eq(STS_ACCOUNTS_API_CONTEXT))).thenReturn(false);

        extension.initialize(context);

        verify(vault).resolveSecret(eq("test-api-key"));
        verify(apiAuthenticationRegistry).register(eq(STS_ACCOUNTS_API_CONTEXT), isA(TokenBasedAuthenticationService.class));
        verify(webService).registerResource(eq(STS_ACCOUNTS), isA(StsAccountsApiController.class));
        verify(webService).registerResource(eq(STS_ACCOUNTS), isA(AuthenticationRequestFilter.class));
    }

    @Test
    void initialize_otherAuthServicePresent_withApiKeyInConfig(StsAccountsApiExtension extension, ServiceExtensionContext context) {
        var config = mock(Config.class);
        when(config.getString(eq(STS_ACCOUNTS_API_KEY)))
                .thenReturn("test-api-key");
        when(context.getConfig()).thenReturn(config);
        when(apiAuthenticationRegistry.hasService(eq(STS_ACCOUNTS_API_CONTEXT))).thenReturn(true);

        extension.initialize(context);

        verify(apiAuthenticationRegistry).hasService(eq(STS_ACCOUNTS_API_CONTEXT));
        verify(webService).registerResource(eq(STS_ACCOUNTS), isA(StsAccountsApiController.class));
        verify(webService).registerResource(eq(STS_ACCOUNTS), isA(AuthenticationRequestFilter.class));
        verifyNoMoreInteractions(vault, apiAuthenticationRegistry);
    }
}