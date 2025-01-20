/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH -  initial implementation
 *
 */

package org.eclipse.edc.api.auth.token;

import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationRegistry;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.api.auth.token.TokenBasedAuthenticationExtension.AUTH_API_KEY;
import static org.eclipse.edc.api.auth.token.TokenBasedAuthenticationExtension.AUTH_API_KEY_ALIAS;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class TokenBasedAuthenticationExtensionTest {

    private static final String VAULT_KEY = "foo";

    private final Vault vault = mock();
    private final ApiAuthenticationRegistry apiAuthenticationRegistry = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(Vault.class, vault);
        context.registerService(ApiAuthenticationRegistry.class, apiAuthenticationRegistry);

        when(vault.resolveSecret(VAULT_KEY)).thenReturn("foo");
    }

    @Test
    public void tokenBasedProvider(TokenBasedAuthenticationExtension extension) {
        var config = mock(Config.class);
        when(config.getString(AUTH_API_KEY)).thenReturn("key");

        assertThat(extension.tokenBasedProvider(config))
                .isSucceeded().isInstanceOf(TokenBasedAuthenticationService.class);

        verifyNoMoreInteractions(vault);
    }

    @Test
    public void tokenBasedProvider_withAlias(TokenBasedAuthenticationExtension extension) {

        var config = mock(Config.class);

        when(config.getString(AUTH_API_KEY_ALIAS, null)).thenReturn("alias");
        when(vault.resolveSecret("alias")).thenReturn("key");

        assertThat(extension.tokenBasedProvider(config))
                .isSucceeded().isInstanceOf(TokenBasedAuthenticationService.class);

        verify(vault).resolveSecret("alias");


    }
}
