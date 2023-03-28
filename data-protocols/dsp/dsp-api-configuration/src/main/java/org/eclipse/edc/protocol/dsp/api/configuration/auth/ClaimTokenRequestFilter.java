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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Intercepts all incoming requests and authenticates them using the IdentityService. As a ClaimToken
 * instance is required for further processing of the claims, the ClaimToken returned by the
 * IdentityService is serialized and added as an additional header.
 */
public class ClaimTokenRequestFilter implements ContainerRequestFilter {
    
    public static final String CLAIM_TOKEN_HEADER = "edcClaimToken";
    private static final String AUTH_HEADER = "Authorization";
    
    private IdentityService identityService;
    private ObjectMapper mapper;
    private String dspWebhookAddress;
    
    public ClaimTokenRequestFilter(IdentityService identityService, ObjectMapper mapper, String dspWebhookAddress) {
        this.identityService = identityService;
        this.mapper = mapper;
        this.dspWebhookAddress = dspWebhookAddress;
    }
    
    /**
     * Checks whether an incoming request is authenticated. If it is authenticated, the resulting
     * ClaimToken is serialized and added as a header with name {@link #CLAIM_TOKEN_HEADER}. If the
     * request is not authenticated, an AuthenticationFailedException is thrown.
     *
     * @param requestContext request context.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        var headers = requestContext.getHeaders().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        var authResult = isAuthenticated(headers);
        if (authResult == null) {
            throw new AuthenticationFailedException();
        }
        
        var claimToken = authResult.getContent();
        requestContext.getHeaders().add(CLAIM_TOKEN_HEADER, writeClaimToken(claimToken));
    }
    
    private Result<ClaimToken> isAuthenticated(Map<String, List<String>> headers) {
        var authHeaders = headers.get(AUTH_HEADER);
        
        return authHeaders.stream()
                .map(this::checkToken)
                .filter(Result::succeeded)
                .findFirst().orElse(null);
    }
    
    private Result<ClaimToken> checkToken(String token) {
        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(token)
                .build();
        
        return identityService.verifyJwtToken(tokenRepresentation, dspWebhookAddress);
    }
    
    private String writeClaimToken(ClaimToken token) {
        try {
            return mapper.writeValueAsString(token);
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
    }
}
