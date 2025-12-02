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

package org.eclipse.edc.api.authentication.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.ServiceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.api.authentication.filter.Constants.REQUEST_PROPERTY_CLAIMS;
import static org.eclipse.edc.api.authentication.filter.Constants.TOKEN_CLAIM_PARTICIPANT_CONTEXT_ID;
import static org.eclipse.edc.api.authentication.filter.Constants.TOKEN_CLAIM_ROLE;
import static org.eclipse.edc.api.authentication.filter.Constants.TOKEN_CLAIM_SCOPE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ServicePrincipalAuthenticationFilterTest {

    private final ParticipantContextService participantContextService = mock();
    private final ServicePrincipalAuthenticationFilter filter = new ServicePrincipalAuthenticationFilter(participantContextService);

    @BeforeEach
    void setup() {
        when(participantContextService.getParticipantContext(anyString()))
                .thenAnswer(i ->
                        ServiceResult.success(ParticipantContext.Builder.newInstance()
                                .identity(i.getArgument(0))
                                .participantContextId(i.getArgument(0))
                                .build()));
    }

    @Test
    void filter_success() {
        var request = mock(ContainerRequestContext.class);
        when(request.getProperty(REQUEST_PROPERTY_CLAIMS)).thenReturn(ClaimToken.Builder.newInstance()
                .claim(TOKEN_CLAIM_SCOPE, "management-api:read")
                .claim(TOKEN_CLAIM_ROLE, ParticipantPrincipal.ROLE_PARTICIPANT)
                .claim(TOKEN_CLAIM_PARTICIPANT_CONTEXT_ID, "test-context-id")
                .build());


        filter.filter(request);

        verify(request).getProperty(REQUEST_PROPERTY_CLAIMS);
        verify(request).setSecurityContext(argThat(sc -> sc.getUserPrincipal() instanceof ParticipantPrincipal));
        verifyNoMoreInteractions(request);
    }

    @Test
    void filter_success_noParticipantContextIdClaim() {
        var request = mock(ContainerRequestContext.class);
        when(request.getProperty(REQUEST_PROPERTY_CLAIMS)).thenReturn(ClaimToken.Builder.newInstance()
                .claim(TOKEN_CLAIM_SCOPE, "management-api:read")
                .claim(TOKEN_CLAIM_ROLE, ParticipantPrincipal.ROLE_PARTICIPANT)
                // missing: participant_context_id claim
                .build());


        filter.filter(request);

        verify(request).getProperty(REQUEST_PROPERTY_CLAIMS);
        verify(request).setSecurityContext(argThat(sc -> sc.getUserPrincipal() instanceof ParticipantPrincipal));
        verifyNoMoreInteractions(request);
    }

    @Test
    void filter_claimsNotPresent() {
        var request = mock(ContainerRequestContext.class);
        when(request.getProperty(REQUEST_PROPERTY_CLAIMS)).thenReturn(null);

        filter.filter(request);
        verify(request).abortWith(argThat(response -> response.getStatus() == 401));
    }

    @Test
    void filter_userNotResolved() {
        when(participantContextService.getParticipantContext(anyString())).thenReturn(ServiceResult.notFound("test message"));
        var request = mock(ContainerRequestContext.class);
        when(request.getProperty(REQUEST_PROPERTY_CLAIMS)).thenReturn(ClaimToken.Builder.newInstance()
                .claim(TOKEN_CLAIM_SCOPE, "management-api:read")
                .claim(TOKEN_CLAIM_ROLE, ParticipantPrincipal.ROLE_PARTICIPANT)
                .claim(TOKEN_CLAIM_PARTICIPANT_CONTEXT_ID, "test-context-id")
                .build());
        filter.filter(request);

        verify(request).abortWith(argThat(response -> response.getStatus() == 401));
    }
}