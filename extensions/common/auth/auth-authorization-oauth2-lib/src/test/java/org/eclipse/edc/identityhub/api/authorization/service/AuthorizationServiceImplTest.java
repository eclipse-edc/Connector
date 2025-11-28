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

package org.eclipse.edc.identityhub.api.authorization.service;

import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.participantcontext.spi.types.AbstractParticipantResource;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.junit.jupiter.api.Test;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AuthorizationServiceImplTest {
    private final AuthorizationServiceImpl authorizationService = new AuthorizationServiceImpl();

    @Test
    void authorize_success() {
        authorizationService.addLookupFunction(TestResource.class, (owner, id) -> new AbstractParticipantResource() {
            @Override
            public String getParticipantContextId() {
                return "test-id";
            }
        });

        var principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test-id");
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        assertThat(authorizationService.authorize(securityContext, "test-id", "test-resource-id", TestResource.class))
                .isSucceeded();
    }

    @Test
    void authorize_whenNoLookupFunction() {
        var principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test-id");
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        assertThat(authorizationService.authorize(securityContext, "test-id", "test-resource-id", TestResource.class))
                .isFailed();
    }

    @Test
    void authorize_whenResourceNotFound() {
        authorizationService.addLookupFunction(TestResource.class, (owner, id) -> null);

        var principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test-id");
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        assertThat(authorizationService.authorize(securityContext, "test-id", "test-resource-id", TestResource.class))
                .isFailed()
                .satisfies(f -> assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND));
    }

    @Test
    void authorize_whenNotAuthorized() {
        authorizationService.addLookupFunction(TestResource.class, (owner, id) -> new AbstractParticipantResource() {
            @Override
            public String getParticipantContextId() {
                return "another-test-id";
            }
        });
        var principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test-id");
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        assertThat(authorizationService.authorize(securityContext, "test-id", "test-resource-id", TestResource.class))
                .isFailed();
    }

    @Test
    void isAuthorized_whenRoleIsAdmin() {
        authorizationService.addLookupFunction(TestResource.class, (owner, id) -> new AbstractParticipantResource() {
            @Override
            public String getParticipantContextId() {
                return "test-id";
            }
        });
        var principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test-id");
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);

        when(securityContext.isUserInRole(eq("admin"))).thenReturn(true);

        assertThat(authorizationService.authorize(securityContext, "test-id", "test-resource-id", TestResource.class))
                .isSucceeded();

        verify(securityContext).isUserInRole(eq("admin"));
        verify(securityContext).getUserPrincipal();
        verifyNoMoreInteractions(securityContext);
    }

    @Test
    void authorize_whenNoOwner() {
        authorizationService.addLookupFunction(TestResource.class, (owner, id) -> new AbstractParticipantResource() {
            @Override
            public String getParticipantContextId() {
                return "test-id";
            }
        });
        var principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test-id");
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);

        assertThat(authorizationService.authorize(securityContext, null, "test-resource-id", TestResource.class))
                .isFailed()
                .satisfies(f -> assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.UNAUTHORIZED));
    }

    @Test
    void authorize_whenOwnerDoesNotOwnResource() {
        authorizationService.addLookupFunction(TestResource.class, (owner, id) -> new AbstractParticipantResource() {
            @Override
            public String getParticipantContextId() {
                return "another-owner-id";
            }
        });

        var principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test-id");
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        assertThat(authorizationService.authorize(securityContext, "test-id", "test-resource-id", TestResource.class))
                .isFailed()
                .satisfies(f -> assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.UNAUTHORIZED));
    }

    @Test
    void authorize_whenPrincipalNotOwnerResource() {
        authorizationService.addLookupFunction(TestResource.class, (owner, id) -> new AbstractParticipantResource() {
            @Override
            public String getParticipantContextId() {
                return "test-id";
            }
        });

        var principal = mock(Principal.class);
        when(principal.getName()).thenReturn("another-owner-id");
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        assertThat(authorizationService.authorize(securityContext, "test-id", "test-resource-id", TestResource.class))
                .isFailed()
                .satisfies(f -> assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.UNAUTHORIZED));
    }

    private static class TestResource extends AbstractParticipantResource {

    }
}