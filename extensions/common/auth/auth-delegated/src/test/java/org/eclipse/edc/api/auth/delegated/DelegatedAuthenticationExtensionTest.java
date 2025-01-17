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

package org.eclipse.edc.api.auth.delegated;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.nimbusds.jose.jwk.source.JWKSourceBuilder.DEFAULT_CACHE_TIME_TO_LIVE;
import static org.eclipse.edc.api.auth.delegated.DelegatedAuthenticationExtension.AUTH_CACHE_VALIDITY_MS;
import static org.eclipse.edc.api.auth.delegated.DelegatedAuthenticationExtension.AUTH_KEY_URL;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DelegatedAuthenticationExtensionTest {

    private final Monitor monitor = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        when(monitor.withPrefix(anyString())).thenReturn(monitor);
        when(context.getMonitor()).thenReturn(monitor);
    }

    @Test
    public void delegatedProvider(DelegatedAuthenticationExtension extension) {

        var config = mock(Config.class);

        when(config.getString(AUTH_KEY_URL)).thenReturn("http://url");

        assertThat(extension.delegatedProvider(monitor, config))
                .isSucceeded().isInstanceOf(DelegatedAuthenticationService.class);


        verify(config).getString(AUTH_KEY_URL);
        verify(config).getLong(AUTH_CACHE_VALIDITY_MS, DEFAULT_CACHE_TIME_TO_LIVE);

    }
}
