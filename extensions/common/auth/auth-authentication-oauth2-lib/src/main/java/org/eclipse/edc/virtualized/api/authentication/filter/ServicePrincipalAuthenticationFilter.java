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
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.spi.iam.ClaimToken;

import java.security.Principal;

import static org.eclipse.edc.virtualized.api.authentication.filter.Constants.REQUEST_PROPERTY_CLAIMS;
import static org.eclipse.edc.virtualized.api.authentication.filter.Constants.TOKEN_CLAIM_PARTICIPANT_CONTEXT_ID;
import static org.eclipse.edc.virtualized.api.authentication.filter.Constants.TOKEN_CLAIM_ROLE;
import static org.eclipse.edc.virtualized.api.authentication.filter.Constants.TOKEN_CLAIM_SCOPE;

/**
 * A {@link ContainerRequestFilter} that extracts a {@link ParticipantPrincipal} from the Authorization Header, specifically,
 * the JWT that is contained in the Authorization Header.
 */
@Priority(Priorities.AUTHENTICATION)
class ServicePrincipalAuthenticationFilter implements ContainerRequestFilter {

    private final ParticipantContextService participantContextService;

    ServicePrincipalAuthenticationFilter(ParticipantContextService participantContextService) {
        this.participantContextService = participantContextService;
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) {

        var claims = containerRequestContext.getProperty(REQUEST_PROPERTY_CLAIMS);
        if (claims == null) {
            containerRequestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED.getStatusCode(), "Authorization failure: no '%s' found".formatted(REQUEST_PROPERTY_CLAIMS)).build());
            return;
        }
        var participantContextId = ((ClaimToken) claims).getStringClaim(TOKEN_CLAIM_PARTICIPANT_CONTEXT_ID);
        var role = ((ClaimToken) claims).getStringClaim(TOKEN_CLAIM_ROLE);
        var scope = ((ClaimToken) claims).getStringClaim(TOKEN_CLAIM_SCOPE);

        if (participantContextId != null) {
            if (participantContextService.getParticipantContext(participantContextId).failed()) {
                containerRequestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED.getStatusCode(), "Authorization Header invalid: participant context not found").build());
                return;
            }
        }

        var servicePrincipal = new ParticipantPrincipal(participantContextId, role, scope);
        containerRequestContext.setSecurityContext(new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return servicePrincipal;
            }

            @Override
            public boolean isUserInRole(String s) {
                return servicePrincipal.getRoles().contains(s);
            }

            @Override
            public boolean isSecure() {
                return containerRequestContext.getUriInfo().getBaseUri().toString().startsWith("https");
            }

            @Override
            public String getAuthenticationScheme() {
                return null;
            }
        });
    }
}
