/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.api.control;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Predicate;

@Deprecated
@Provider
@PreMatching
class HttpApiKeyAuthContainerRequestFilter implements ContainerRequestFilter {
    /**
     * A predicate evaluating eligibility of the ContainerRequestFilter to handle the incoming request
     */
    private final Predicate<ContainerRequestContext> containerRequestContextPredicate;

    /**
     * The name/key of the HTTP header carrying the authentication key
     */
    private final String expectedHeaderName;

    /**
     * The expected API-Key to be present in the incoming request
     */
    private final String expectedHeaderValue;

    public HttpApiKeyAuthContainerRequestFilter(
            @NotNull String expectedHeaderName,
            @NotNull String expectedHeaderValue,
            @NotNull Predicate<ContainerRequestContext> containerRequestContextPredicate) {
        this.expectedHeaderName = Objects.requireNonNull(expectedHeaderName);
        this.expectedHeaderValue = Objects.requireNonNull(expectedHeaderValue);
        this.containerRequestContextPredicate = Objects.requireNonNull(containerRequestContextPredicate);
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        if (!containerRequestContextPredicate.test(containerRequestContext)) {
            return;
        }

        MultivaluedMap<String, String> headers = containerRequestContext.getHeaders();
        
        /*
         * No apiKey has be provided, so abort the request with HTTP status 401 UNAUTHORIZED
         */
        if (!headers.containsKey(expectedHeaderName) || (headers.getFirst(expectedHeaderName) == null)) {
            containerRequestContext.abortWith(unauthorized());
            return;
        }

        /*
         * If the apiKey does not match the configured value abort the request with HTTP status 403 FORBIDDEN
         */
        if (!expectedHeaderValue.equals(headers.getFirst(expectedHeaderName))) {
            containerRequestContext.abortWith(forbidden());
        }
    }

    private static Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    private static Response forbidden() {
        return Response.status(Response.Status.FORBIDDEN).build();
    }
}
