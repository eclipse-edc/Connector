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
 *
 */

package org.eclipse.edc.api.auth.spi;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationRegistry;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationRequestFilterTest {

    private final AuthenticationService authenticationService = mock();
    private final ApiAuthenticationRegistry authenticationRegistry = mock();

    private final AuthenticationRequestFilter filter = new AuthenticationRequestFilter(authenticationRegistry, "context");

    @BeforeEach
    void setUp() {
        when(authenticationRegistry.resolve("context")).thenReturn(authenticationService);
    }

    @Test
    void filter() {
        when(authenticationService.isAuthenticated(anyMap())).thenReturn(true);
        var contextMock = mock(ContainerRequestContext.class);
        when(contextMock.getHeaders()).thenReturn(new MultivaluedHashMap<>(Map.of("foo", "bar")));

        filter.filter(contextMock); //should not throw an exception
        verify(authenticationService).isAuthenticated(anyMap());
    }

    @Test
    void filter_serviceThrowsException() {
        var exc = new AuthenticationFailedException("test");
        when(authenticationService.isAuthenticated(anyMap())).thenThrow(exc);
        var contextMock = mock(ContainerRequestContext.class);

        when(contextMock.getHeaders()).thenReturn(new MultivaluedHashMap<>(Map.of("foo", "bar")));

        assertThatThrownBy(() -> filter.filter(contextMock)).isInstanceOf(AuthenticationFailedException.class).hasMessage("test");
        verify(authenticationService).isAuthenticated(anyMap());
    }


    @Test
    void filter_notAuthorized() {
        when(authenticationService.isAuthenticated(anyMap())).thenReturn(false);
        var contextMock = mock(ContainerRequestContext.class);

        when(contextMock.getHeaders()).thenReturn(new MultivaluedHashMap<>(Map.of("foo", "bar")));

        assertThatThrownBy(() -> filter.filter(contextMock)).isInstanceOf(AuthenticationFailedException.class);
        verify(authenticationService).isAuthenticated(anyMap());
    }

    @Test
    void filter_shouldSkipOnOptions() {
        var contextMock = mock(ContainerRequestContext.class);
        when(contextMock.getMethod()).thenReturn("OPTIONS");

        filter.filter(contextMock);
        verify(authenticationService, never()).isAuthenticated(anyMap());
    }
}
