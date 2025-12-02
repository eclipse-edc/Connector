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
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.junit.jupiter.api.Test;

import java.security.Principal;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ScopeBasedAccessFilterTest {

    private final ScopeBasedAccessFilter filter = new ScopeBasedAccessFilter("required-scope");

    @Test
    void filter_abortsWithForbidden_whenSecurityContextIsNull() throws Exception {
        var request = mock(ContainerRequestContext.class);
        when(request.getSecurityContext()).thenReturn(null);

        filter.filter(request);

        verify(request).getSecurityContext();
        verify(request).abortWith(argThat(r -> r.getStatus() == 403));
        verifyNoMoreInteractions(request);
    }

    @Test
    void filter_abortsWithForbidden_whenPrincipalIsNull() throws Exception {
        var request = mock(ContainerRequestContext.class);
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(null);
        when(request.getSecurityContext()).thenReturn(securityContext);

        filter.filter(request);

        verify(request).getSecurityContext();
        verify(securityContext).getUserPrincipal();
        verify(request).abortWith(argThat(r -> r.getStatus() == 403));
        verifyNoMoreInteractions(request, securityContext);
    }

    @Test
    void filter_rejectsPrincipalNotParticipantPrincipal() throws Exception {
        var request = mock(ContainerRequestContext.class);
        var securityContext = mock(SecurityContext.class);
        var otherPrincipal = mock(Principal.class);
        when(securityContext.getUserPrincipal()).thenReturn(otherPrincipal);
        when(request.getSecurityContext()).thenReturn(securityContext);

        filter.filter(request);

        verify(request).getSecurityContext();
        verify(securityContext).getUserPrincipal();
        verify(request).abortWith(argThat(r -> r.getStatus() == 401));
        verifyNoMoreInteractions(request, securityContext);
    }

    @Test
    void filter_allowsWhenRequiredScopePresent() throws Exception {
        var request = mock(ContainerRequestContext.class);
        var securityContext = mock(SecurityContext.class);
        var participant = new ParticipantPrincipal("some-id", ParticipantPrincipal.ROLE_PARTICIPANT, "foo required-scope bar");
        when(securityContext.getUserPrincipal()).thenReturn(participant);
        when(request.getSecurityContext()).thenReturn(securityContext);

        filter.filter(request);

        verify(request).getSecurityContext();
        verify(securityContext).getUserPrincipal();
        verifyNoMoreInteractions(request, securityContext);
    }

    @Test
    void filter_abortsWithUnauthorized_whenRequiredScopeMissingForParticipant() throws Exception {
        var request = mock(ContainerRequestContext.class);
        var securityContext = mock(SecurityContext.class);
        var participant = new ParticipantPrincipal("some-id", ParticipantPrincipal.ROLE_PARTICIPANT, "foo bar");
        when(securityContext.getUserPrincipal()).thenReturn(participant);
        when(request.getSecurityContext()).thenReturn(securityContext);

        filter.filter(request);

        verify(request).getSecurityContext();
        verify(securityContext).getUserPrincipal();
        verify(request).abortWith(argThat(r -> r.getStatus() == 403));
        verifyNoMoreInteractions(request, securityContext);
    }

    @Test
    void filter_abortsWithUnauthorized_whenParticipantScopeIsEmpty() throws Exception {
        var request = mock(ContainerRequestContext.class);
        var securityContext = mock(SecurityContext.class);
        var participant = new ParticipantPrincipal("some-id", ParticipantPrincipal.ROLE_PARTICIPANT, " ");
        when(securityContext.getUserPrincipal()).thenReturn(participant);
        when(request.getSecurityContext()).thenReturn(securityContext);

        filter.filter(request);

        verify(request).getSecurityContext();
        verify(securityContext).getUserPrincipal();
        verify(request).abortWith(argThat(r -> r.getStatus() == 403));
        verifyNoMoreInteractions(request, securityContext);
    }

    @Test
    void filter_doesNotMatchPartialNames() throws Exception {
        var request = mock(ContainerRequestContext.class);
        var securityContext = mock(SecurityContext.class);
        // include similar token "required-scope-extra" to ensure exact token matching is required
        var participant = new ParticipantPrincipal("some-id", ParticipantPrincipal.ROLE_PARTICIPANT, "required-scope-extra");
        when(securityContext.getUserPrincipal()).thenReturn(participant);
        when(request.getSecurityContext()).thenReturn(securityContext);

        filter.filter(request);

        verify(request).getSecurityContext();
        verify(securityContext).getUserPrincipal();
        verify(request).abortWith(argThat(r -> r.getStatus() == 403));
        verifyNoMoreInteractions(request, securityContext);
    }


}
