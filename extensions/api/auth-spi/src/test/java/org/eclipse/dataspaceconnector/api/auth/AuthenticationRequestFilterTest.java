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

package org.eclipse.dataspaceconnector.api.auth;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.eclipse.dataspaceconnector.spi.exception.AuthenticationFailedException;
import org.eclipse.dataspaceconnector.spi.exception.NotAuthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationRequestFilterTest {

    private AuthenticationRequestFilter filter;
    private AuthenticationService authSrvMock;

    @BeforeEach
    void setUp() {
        authSrvMock = mock(AuthenticationService.class);
        filter = new AuthenticationRequestFilter(authSrvMock);
    }

    @Test
    void filter() {
        when(authSrvMock.isAuthenticated(anyMap())).thenReturn(true);
        var contextMock = mock(ContainerRequestContext.class);
        when(contextMock.getHeaders()).thenReturn(new MultivaluedHashMap<>(Map.of("foo", "bar")));

        filter.filter(contextMock); //should not throw an exception
        verify(authSrvMock).isAuthenticated(anyMap());
    }

    @Test
    void filter_serviceThrowsException() throws IOException {
        var exc = new AuthenticationFailedException("test");
        when(authSrvMock.isAuthenticated(anyMap())).thenThrow(exc);
        var contextMock = mock(ContainerRequestContext.class);

        when(contextMock.getHeaders()).thenReturn(new MultivaluedHashMap<>(Map.of("foo", "bar")));

        assertThatThrownBy(() -> filter.filter(contextMock)).isInstanceOf(AuthenticationFailedException.class).hasMessage("test");
        verify(authSrvMock).isAuthenticated(anyMap());
    }


    @Test
    void filter_notAuthorized() {
        when(authSrvMock.isAuthenticated(anyMap())).thenReturn(false);
        var contextMock = mock(ContainerRequestContext.class);

        when(contextMock.getHeaders()).thenReturn(new MultivaluedHashMap<>(Map.of("foo", "bar")));

        assertThatThrownBy(() -> filter.filter(contextMock)).isInstanceOf(NotAuthorizedException.class);
        verify(authSrvMock).isAuthenticated(anyMap());
    }

    @Test
    void filter_shouldSkipOnOptions() {
        var contextMock = mock(ContainerRequestContext.class);
        when(contextMock.getMethod()).thenReturn("OPTIONS");

        filter.filter(contextMock);
        verify(authSrvMock, never()).isAuthenticated(anyMap());
    }
}