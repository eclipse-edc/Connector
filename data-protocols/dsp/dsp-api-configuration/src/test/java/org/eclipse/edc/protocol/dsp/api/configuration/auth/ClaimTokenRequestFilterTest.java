/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.api.configuration.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.protocol.dsp.api.configuration.auth.ClaimTokenRequestFilter.CLAIM_TOKEN_HEADER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClaimTokenRequestFilterTest {
    
    private static final String WEBHOOK_ADDRESS = "http://webhook";
    private static final String AUTH_HEADER = "auth";
    
    private IdentityService identityService = mock(IdentityService.class);
    private ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
    private MultivaluedMap<String, String> headers = mock(MultivaluedMap.class);
    
    private ClaimTokenRequestFilter claimTokenRequestFilter;
    
    @BeforeEach
    void setUp() {
        claimTokenRequestFilter = new ClaimTokenRequestFilter(identityService, WEBHOOK_ADDRESS);
        
        when(headers.entrySet()).thenReturn(Set.of(Map.entry("Authorization", List.of(AUTH_HEADER))));
        when(requestContext.getHeaders()).thenReturn(headers);
    }
    
    @Test
    void filter_authenticationSuccessful_addClaimTokenHeader() throws IOException {
        var claimToken = ClaimToken.Builder.newInstance().claim("key", "value").build();
        when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.success(claimToken));
        
        claimTokenRequestFilter.filter(requestContext);
        
        verify(identityService).verifyJwtToken(argThat(tr -> tr.getToken().equals(AUTH_HEADER)), eq(WEBHOOK_ADDRESS));
        verify(headers).add(eq(CLAIM_TOKEN_HEADER), eq(new ObjectMapper().writeValueAsString(claimToken)));
    }
    
    @Test
    void filter_authenticationFailed_throwException() {
        when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.failure("error"));
        
        assertThatThrownBy(() -> claimTokenRequestFilter.filter(requestContext))
                .isInstanceOf(AuthenticationFailedException.class);
        
        verify(identityService).verifyJwtToken(argThat(tr -> tr.getToken().equals(AUTH_HEADER)), eq(WEBHOOK_ADDRESS));
        verify(headers, never()).add(eq(CLAIM_TOKEN_HEADER), anyString());
    }
}
