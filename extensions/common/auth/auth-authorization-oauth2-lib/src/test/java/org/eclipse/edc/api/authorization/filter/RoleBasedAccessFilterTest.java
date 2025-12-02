/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.api.authorization.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class RoleBasedAccessFilterTest {

    private final RoleBasedAccessFilter filter = new RoleBasedAccessFilter("role1", "role2");

    @Test
    void filter_whenHasRole() {
        var request = mock(ContainerRequestContext.class);
        var securityContext = mock(SecurityContext.class);
        when(securityContext.isUserInRole(eq("role1"))).thenReturn(true);
        when(request.getSecurityContext()).thenReturn(securityContext);
        filter.filter(request);

        verify(request).getSecurityContext();
        verify(securityContext).isUserInRole(anyString());
        verifyNoMoreInteractions(request, securityContext);
    }

    @Test
    void filter_whenNoRole() {
        var request = mock(ContainerRequestContext.class);
        var securityContext = mock(SecurityContext.class);
        when(securityContext.isUserInRole(eq("role1"))).thenReturn(false);
        when(request.getSecurityContext()).thenReturn(securityContext);
        filter.filter(request);

        verify(request).getSecurityContext();
        verify(securityContext, times(2)).isUserInRole(anyString());
        verify(request).abortWith(argThat(r -> r.getStatus() == 403));
        verifyNoMoreInteractions(request, securityContext);
    }
}