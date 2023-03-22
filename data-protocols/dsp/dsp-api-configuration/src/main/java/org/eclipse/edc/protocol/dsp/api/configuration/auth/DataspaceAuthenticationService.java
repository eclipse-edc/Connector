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

import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;

import java.util.List;
import java.util.Map;

public class DataspaceAuthenticationService implements AuthenticationService {
    
    private static final String AUTH_HEADER = "Authorization";
    
    private IdentityService identityService;
    private String dspWebhookAddress;
    
    public DataspaceAuthenticationService(IdentityService identityService, String dspWebhookAddress) {
        this.identityService = identityService;
        this.dspWebhookAddress = dspWebhookAddress;
    }
    
    @Override
    public boolean isAuthenticated(Map<String, List<String>> headers) {
        return headers.keySet().stream()
                .filter(AUTH_HEADER::equals)
                .map(headers::get)
                .filter(list -> !list.isEmpty())
                .anyMatch(list -> list.stream()
                        .anyMatch(this::checkToken));
    }
    
    private boolean checkToken(String token) {
        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(token)
                .build();
        
        var result = identityService.verifyJwtToken(tokenRepresentation, dspWebhookAddress);
        
        return result.succeeded();
    }
}
