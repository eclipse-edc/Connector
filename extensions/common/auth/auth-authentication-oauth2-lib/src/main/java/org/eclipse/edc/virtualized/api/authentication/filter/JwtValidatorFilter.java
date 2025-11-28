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

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationService;

import java.util.List;

import static org.eclipse.edc.virtualized.api.authentication.filter.Constants.REQUEST_PROPERTY_CLAIMS;

/**
 * Validates the JWT signature against the IdP's public key and validates basic claims, such as {@code iss} and {@code exp}.
 */
@PreMatching
@Priority(Priorities.AUTHENTICATION)
class JwtValidatorFilter implements ContainerRequestFilter {

    private final TokenValidationService tokenValidationService;
    private final PublicKeyResolver publicKeyResolver;
    private final List<TokenValidationRule> rules;

    JwtValidatorFilter(TokenValidationService tokenValidationService, PublicKeyResolver publicKeyResolver, List<TokenValidationRule> rules) {
        this.publicKeyResolver = publicKeyResolver;
        this.rules = rules;
        this.tokenValidationService = tokenValidationService;
    }


    @Override
    public void filter(ContainerRequestContext requestContext) {
        var authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            abort(requestContext, "Missing Authorization header");
            return;
        }

        var token = authHeader.substring("Bearer ".length()).trim();

        var tokenValidationResult = tokenValidationService.validate(token, publicKeyResolver, rules);

        if (tokenValidationResult.failed()) {
            abort(requestContext, tokenValidationResult.getFailureDetail());
            return;
        }

        requestContext.setProperty(REQUEST_PROPERTY_CLAIMS, tokenValidationResult.getContent());
    }


    private void abort(ContainerRequestContext ctx, String message) {
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"error\":\"" + message + "\"}")
                .build());
    }
}
