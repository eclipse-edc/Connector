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

package org.eclipse.dataspaceconnector.api.auth;

import org.eclipse.dataspaceconnector.junit.extensions.DependencyInjectionExtension;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class TokenBasedAuthenticationExtensionTest {

    private TokenBasedAuthenticationExtension extension;

    private static final String AUTH_SETTING_APIKEY = "edc.api.auth.key";

    private static final String AUTH_SETTING_APIKEY_ALIAS = "edc.api.auth.key.alias";

    private static final String VAULT_KEY = "foo";

    private Vault vaultMock;
    private ServiceExtensionContext serviceExtensionContextMock;

    @BeforeEach
    void setup(ServiceExtensionContext context, ObjectFactory factory) {

        serviceExtensionContextMock = spy(context); //used to inject the config
        vaultMock = mock(Vault.class);

        context.registerService(Vault.class, vaultMock);
        context.registerService(ServiceExtensionContext.class, serviceExtensionContextMock);

        when(vaultMock.resolveSecret(VAULT_KEY)).thenReturn("foo");

        extension = factory.constructInstance(TokenBasedAuthenticationExtension.class);
    }

    @Test
    public void testPrimaryMethod_loadKeyFromVault() {
        setAuthSettingApiKeyAlias(VAULT_KEY);
        setAuthSettingApiKey("bar");

        extension.initialize(serviceExtensionContextMock);

        verify(serviceExtensionContextMock, never())
                .getSetting(eq(AUTH_SETTING_APIKEY), anyString());

        verify(serviceExtensionContextMock)
                .getSetting(AUTH_SETTING_APIKEY_ALIAS, null);

        verify(vaultMock).resolveSecret(VAULT_KEY);
    }

    @Test
    public void testSecondaryMethod_loadKeyFromConfig() {

        setAuthSettingApiKeyAlias(null);
        setAuthSettingApiKey("bar");

        extension.initialize(serviceExtensionContextMock);

        verify(serviceExtensionContextMock)
                .getSetting(eq(AUTH_SETTING_APIKEY), anyString());

        verify(serviceExtensionContextMock)
                .getSetting(AUTH_SETTING_APIKEY_ALIAS, null);

        verify(vaultMock, never()).resolveSecret(anyString());
    }

    private void setAuthSettingApiKey(String value) {
        when(serviceExtensionContextMock.getSetting(eq(AUTH_SETTING_APIKEY), anyString()))
                .thenReturn(value);
    }

    private void setAuthSettingApiKeyAlias(String value) {
        when(serviceExtensionContextMock.getSetting(eq(AUTH_SETTING_APIKEY_ALIAS), isNull()))
                .thenReturn(value);
    }
}
