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
import org.eclipse.dataspaceconnector.api.exception.AuthorizationFailedException;
import org.eclipse.dataspaceconnector.api.exception.NotAuthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorizationRequestFilterTest {

    private AuthorizationRequestFilter filter;
    private AuthorizationService authSrvMock;

    @BeforeEach
    void setUp() {
        authSrvMock = mock(AuthorizationService.class);
        filter = new AuthorizationRequestFilter(authSrvMock);
    }

    @Test
    void filter() {
        when(authSrvMock.isAuthorized(anyMap())).thenReturn(true);
        var contextMock = mock(ContainerRequestContext.class);
        when(contextMock.getHeaders()).thenReturn(new MultivaluedHashMap<>(Map.of("foo", "bar")));

        filter.filter(contextMock); //should not throw an exception
        verify(authSrvMock).isAuthorized(anyMap());
    }

    @Test
    void filter_serviceThrowsException() throws IOException {
        var exc = new AuthorizationFailedException("test");
        when(authSrvMock.isAuthorized(anyMap())).thenThrow(exc);
        var contextMock = mock(ContainerRequestContext.class);

        when(contextMock.getHeaders()).thenReturn(new MultivaluedHashMap<>(Map.of("foo", "bar")));

        assertThatThrownBy(() -> filter.filter(contextMock)).isInstanceOf(AuthorizationFailedException.class).hasMessage("test");
        verify(authSrvMock).isAuthorized(anyMap());
    }


    @Test
    void filter_notAuthorized() {
        when(authSrvMock.isAuthorized(anyMap())).thenReturn(false);
        var contextMock = mock(ContainerRequestContext.class);

        when(contextMock.getHeaders()).thenReturn(new MultivaluedHashMap<>(Map.of("foo", "bar")));

        assertThatThrownBy(() -> filter.filter(contextMock)).isInstanceOf(NotAuthorizedException.class);
        verify(authSrvMock).isAuthorized(anyMap());
    }
}