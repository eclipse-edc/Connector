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

package org.eclipse.edc.api.auth.spi;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationRegistry;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;

import java.util.Map;

import static jakarta.ws.rs.HttpMethod.OPTIONS;
import static java.util.stream.Collectors.toMap;

/**
 * Intercepts all requests sent to this resource and authenticates them using an {@link AuthenticationService}. In order
 * to be able to handle CORS requests properly, OPTIONS requests are not validated as their headers usually don't
 * contain credentials.
 */
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationRequestFilter implements ContainerRequestFilter {

    private final ApiAuthenticationRegistry authenticationRegistry;
    private final String context;

    public AuthenticationRequestFilter(ApiAuthenticationRegistry authenticationRegistry, String context) {
        this.authenticationRegistry = authenticationRegistry;
        this.context = context;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {

        // OPTIONS requests don't have credentials - do not authenticate
        if (!OPTIONS.equalsIgnoreCase(requestContext.getMethod())) {
            var headers = requestContext.getHeaders().entrySet().stream()
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
            var isAuthenticated = authenticationRegistry.resolve(context).isAuthenticated(headers);
            if (!isAuthenticated) {
                throw new AuthenticationFailedException();
            }
        }
    }
}
