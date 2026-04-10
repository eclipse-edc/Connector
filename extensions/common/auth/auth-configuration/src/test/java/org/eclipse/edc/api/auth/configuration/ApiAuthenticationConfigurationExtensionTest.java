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

import org.eclipse.edc.api.auth.spi.ApiAuthenticationProvider;
import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationProviderRegistry;
import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationRegistry;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class ApiAuthenticationConfigurationExtensionTest {

    private final Monitor monitor = mock();
    private final ApiAuthenticationRegistry authenticationRegistry = mock();
    private final ApiAuthenticationProviderRegistry providerRegistry = mock();

    @BeforeEach
    void setUp(TestExtensionContext context) {
        context.registerService(ApiAuthenticationRegistry.class, authenticationRegistry);
        context.registerService(ApiAuthenticationProviderRegistry.class, providerRegistry);
        context.registerService(Monitor.class, monitor);
    }

    @Test
    void prepare(ObjectFactory factory, TestExtensionContext context) {
        var authType = "testAuth";
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "web.http.test.auth.type", authType,
                "web.http.test.auth.custom", "custom",
                "web.http.test.auth.context", "test-ctx")));
        var provider = mock(ApiAuthenticationProvider.class);
        var authentication = mock(AuthenticationService.class);
        when(providerRegistry.resolve(authType)).thenReturn(provider);
        when(provider.provide(any())).thenReturn(Result.success(authentication));

        var extension = factory.constructInstance(ApiAuthenticationConfigurationExtension.class);
        extension.prepare();

        verify(providerRegistry).resolve(authType);
        verify(provider).provide(any());
        verify(authenticationRegistry).register("test", authentication);
    }

    @Test
    void prepare_whenNoAuthSet(ObjectFactory factory, TestExtensionContext context) {
        var authType = "testAuth";
        var provider = mock(ApiAuthenticationProvider.class);
        var authentication = mock(AuthenticationService.class);
        when(providerRegistry.resolve(authType)).thenReturn(provider);
        when(provider.provide(any())).thenReturn(Result.success(authentication));
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "web.http.test.port", "9999")));

        var extension = factory.constructInstance(ApiAuthenticationConfigurationExtension.class);
        extension.prepare();

        verifyNoMoreInteractions(providerRegistry);
        verifyNoMoreInteractions(provider);
        verifyNoMoreInteractions(authenticationRegistry);
    }

    @Test
    void prepare_whenNoProvider(ObjectFactory factory, TestExtensionContext context) {
        var authType = "testAuth";
        var provider = mock(ApiAuthenticationProvider.class);
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "web.http.test.auth.type", authType,
                "web.http.test.auth.custom", "custom"))
        );
        when(providerRegistry.resolve(authType)).thenReturn(null);

        var extension = factory.constructInstance(ApiAuthenticationConfigurationExtension.class);

        assertThatThrownBy(extension::prepare).isInstanceOf(EdcException.class).hasMessageContaining(authType);

        verify(providerRegistry).resolve(authType);
        verifyNoMoreInteractions(provider);
        verifyNoMoreInteractions(authenticationRegistry);
    }

    @Test
    void prepare_whenProviderFails(ObjectFactory factory, TestExtensionContext context) {
        var authType = "testAuth";
        context.setConfig(ConfigFactory.fromMap(Map.of(
                "web.http.test.auth.type", authType,
                "web.http.test.auth.custom", "custom"))
        );
        var provider = mock(ApiAuthenticationProvider.class);
        when(providerRegistry.resolve(authType)).thenReturn(provider);
        when(provider.provide(any())).thenReturn(Result.failure("Auth failure"));

        var extension = factory.constructInstance(ApiAuthenticationConfigurationExtension.class);

        assertThatThrownBy(extension::prepare).isInstanceOf(EdcException.class).hasMessageContaining("Auth failure");

        verify(providerRegistry).resolve(authType);
        verify(provider).provide(any());
        verifyNoMoreInteractions(authenticationRegistry);
    }

}
