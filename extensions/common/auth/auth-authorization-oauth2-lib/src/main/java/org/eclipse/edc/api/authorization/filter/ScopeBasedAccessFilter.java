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

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;

import java.io.IOException;
import java.util.Arrays;

@Priority(Priorities.AUTHORIZATION)
class ScopeBasedAccessFilter implements ContainerRequestFilter {
    private final String requiredScope;

    ScopeBasedAccessFilter(String requiredScope) {
        this.requiredScope = requiredScope;
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        var securityContext = containerRequestContext.getSecurityContext();
        if (securityContext == null) {
            containerRequestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
            return;
        }

        var principal = securityContext.getUserPrincipal();
        if (principal == null) {
            containerRequestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
            return;
        }
        if (principal instanceof ParticipantPrincipal participantPrincipal) {
            var matches = Arrays.asList(participantPrincipal.scope().split(" ")).contains(requiredScope);
            if (!matches) {
                containerRequestContext.abortWith(Response.status(Response.Status.FORBIDDEN.getStatusCode(), "Required scope '%s' missing".formatted(requiredScope)).build());
            }
            // else: continue request normally
        } else {
            containerRequestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }

    }
}
