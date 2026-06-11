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

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.api.auth.spi.ManagementApiScopes;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.api.auth.spi.ScopeMatcher;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;

import java.security.Principal;
import java.util.List;

import static org.eclipse.edc.api.authentication.filter.Constants.REQUEST_PROPERTY_CLAIMS;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SCOPE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;

/**
 * A {@link ContainerRequestFilter} that extracts a {@link ParticipantPrincipal} from the Authorization Header, specifically,
 * the JWT that is contained in the Authorization Header. The principal's identity is taken from the standard {@code sub}
 * claim and its permissions from the {@code scope} claim.
 */
@Priority(Priorities.AUTHENTICATION)
public class ServicePrincipalAuthenticationFilter implements ContainerRequestFilter {

    private final ParticipantContextService participantContextService;
    private final ScopeMatcher scopeMatcher = new ScopeMatcher();
    private final List<String> adminScopes;

    public ServicePrincipalAuthenticationFilter(ParticipantContextService participantContextService) {
        this(participantContextService, ManagementApiScopes.ADMIN);
    }

    /**
     * Creates a filter whose admin elevation is conferred by the supplied scopes.
     *
     * @param adminScopes the scopes that convey admin elevation. A token whose granted scope satisfies any of these is
     *                    treated as an (elevated) service account, so its {@code sub} need not reference an existing
     *                    participant context. Defaults to {@link ManagementApiScopes#ADMIN}.
     */
    public ServicePrincipalAuthenticationFilter(ParticipantContextService participantContextService, String... adminScopes) {
        this.participantContextService = participantContextService;
        this.adminScopes = List.of(adminScopes);
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) {

        var claims = containerRequestContext.getProperty(REQUEST_PROPERTY_CLAIMS);
        if (claims instanceof ClaimToken claimToken) {
            var subject = claimToken.getStringClaim(SUBJECT);
            var scope = claimToken.getStringClaim(SCOPE);

            // a non-admin principal is identified by its participant context (read from the 'sub' claim), which must exist;
            // an admin principal is elevated, so its subject need not correspond to a participant context (e.g. a service account)
            var authorized = isAuthorized(scope, subject);
            if (authorized.failed()) {
                containerRequestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).entity("Not authorized: %s".formatted(authorized.getFailureDetail())).build());
                return;
            }

            var servicePrincipal = new ParticipantPrincipal(subject, scope);
            containerRequestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return servicePrincipal;
                }

                @Override
                public boolean isUserInRole(String s) {
                    return false;
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
        } else {
            containerRequestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED.getStatusCode(), "Authorization failure: no '%s' found".formatted(REQUEST_PROPERTY_CLAIMS)).build());
        }

    }

    /**
     * Validates if a subject is authorized for a given scope. This is the case if the scope is an "admin" scope, in
     * which case the subject claim is ignored, or if the scope is not admin, and the subject claim references an existing
     * participant context
     *
     * @param scope   The scope for which authorization is being requested.
     * @param subject The subject claim for which authorization is being verified.
     * @return A {@link Result} indicating the authorization result.
     */
    private Result<Void> isAuthorized(String scope, String subject) {

        if (isAdmin(scope)) {
            return Result.success();
        }
        if (subject == null) {
            return Result.failure("No 'sub' claim present");
        }
        if (participantContextService.getParticipantContext(subject).failed()) {
            return Result.failure("No participant for 'sub = %s' found".formatted(subject));
        }
        return Result.success();
    }

    private boolean isAdmin(String grantedScopes) {
        return adminScopes.stream().anyMatch(adminScope -> scopeMatcher.isSatisfiedBy(adminScope, grantedScopes));
    }
}
