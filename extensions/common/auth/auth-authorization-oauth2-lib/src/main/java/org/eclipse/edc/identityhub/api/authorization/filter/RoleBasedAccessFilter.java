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

package org.eclipse.edc.identityhub.api.authorization.filter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;

import java.util.Arrays;
import java.util.List;

/**
 * This filter is registered for controller methods annotated with {@link jakarta.annotation.security.RolesAllowed} and checks, if
 * the current {@link jakarta.ws.rs.core.SecurityContext} fulfills the role requirement of that method.
 */
@Priority(Priorities.AUTHORIZATION)
class RoleBasedAccessFilter implements ContainerRequestFilter {
    private final List<String> allowedRoles;

    RoleBasedAccessFilter(String... allowedRoles) {
        this.allowedRoles = Arrays.asList(allowedRoles);
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) {
        var securityContext = containerRequestContext.getSecurityContext();
        // only continue if user has the correct roles
        if (allowedRoles.stream().noneMatch(securityContext::isUserInRole)) {
            containerRequestContext.abortWith(Response.status(Response.Status.FORBIDDEN).entity("Required user role not satisfied.").build());
        }
    }

}
