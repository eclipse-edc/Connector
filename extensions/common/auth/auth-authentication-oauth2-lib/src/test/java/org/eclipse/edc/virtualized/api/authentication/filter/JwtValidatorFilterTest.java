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

package org.eclipse.edc.virtualized.api.authentication.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.eclipse.edc.virtualized.api.authentication.filter.Constants.REQUEST_PROPERTY_CLAIMS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class JwtValidatorFilterTest {

    private final TokenValidationService tokenValidationService = mock(TokenValidationService.class);

    private final JwtValidatorFilter filter = new JwtValidatorFilter(tokenValidationService, mock(), List.of());

    @Test
    void filter_success_setsClaimsProperty() {
        var request = mock(ContainerRequestContext.class);
        when(request.getHeaderString("Authorization")).thenReturn("Bearer valid-token");

        var claims = ClaimToken.Builder.newInstance().build();

        when(tokenValidationService.validate(eq("valid-token"), any(), anyList()))
                .thenReturn(Result.success(claims));

        filter.filter(request);

        verify(request).getHeaderString("Authorization");
        verify(request).setProperty(REQUEST_PROPERTY_CLAIMS, claims);
        verifyNoMoreInteractions(request);
    }

    @Test
    void filter_missingAuthorizationHeader_abortsWith401() {
        var request = mock(ContainerRequestContext.class);
        when(request.getHeaderString("Authorization")).thenReturn(null);

        filter.filter(request);

        verify(request).getHeaderString("Authorization");
        verify(request).abortWith(argThat(response ->
                response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode() &&
                        response.getEntity() instanceof String &&
                        ((String) response.getEntity()).contains("Missing Authorization header")));
        verifyNoMoreInteractions(request);
    }

    @Test
    void filter_nonBearerAuthorization_abortsWith401() {
        var request = mock(ContainerRequestContext.class);
        when(request.getHeaderString("Authorization")).thenReturn("Basic abc");

        filter.filter(request);

        verify(request).getHeaderString("Authorization");
        verify(request).abortWith(argThat(response ->
                response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode() &&
                        response.getEntity() instanceof String &&
                        ((String) response.getEntity()).contains("Missing Authorization header")));
        verifyNoMoreInteractions(request);
    }

    @Test
    void filter_tokenValidationFailed_abortsWithFailureDetail() {
        var request = mock(ContainerRequestContext.class);
        when(request.getHeaderString("Authorization")).thenReturn("Bearer bad-token");

        when(tokenValidationService.validate(eq("bad-token"), any(), anyList()))
                .thenReturn(Result.failure("invalid token"));

        filter.filter(request);

        verify(request).getHeaderString("Authorization");
        verify(request).abortWith(argThat(response ->
                response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode() &&
                        response.getEntity() instanceof String &&
                        ((String) response.getEntity()).contains("invalid token")));
        verifyNoMoreInteractions(request);
    }
}