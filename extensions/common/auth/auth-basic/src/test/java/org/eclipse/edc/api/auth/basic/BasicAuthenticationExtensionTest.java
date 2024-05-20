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

package org.eclipse.edc.api.auth.basic;

import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationRegistry;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.eclipse.edc.api.auth.basic.BasicAuthenticationExtension.BASIC_AUTH;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class BasicAuthenticationExtensionTest {

    private final ApiAuthenticationRegistry apiAuthenticationRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(ApiAuthenticationRegistry.class, apiAuthenticationRegistry);
    }

    @Test
    void shouldRegisterApiAuthorization(BasicAuthenticationExtension extension, ServiceExtensionContext context) {
        when(context.getConfig(BASIC_AUTH)).thenReturn(ConfigFactory.fromMap(Map.of("edc.api.auth.basic.vault-keys.key", "value")));

        extension.initialize(context);

        verify(apiAuthenticationRegistry).register(eq("management-api"), isA(BasicAuthenticationService.class));
    }

    @Test
    void shouldNoRegisterApiAuthentication_whenNoConfigProvided(BasicAuthenticationExtension extension, ServiceExtensionContext context) {
        when(context.getConfig(BASIC_AUTH)).thenReturn(ConfigFactory.empty());

        extension.initialize(context);

        verifyNoInteractions(apiAuthenticationRegistry);
    }
}
