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

package org.eclipse.edc.identityhub.api.authorization.service;

import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.participantcontext.spi.types.ParticipantResource;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

public class AuthorizationServiceImpl implements AuthorizationService {
    private final Map<Class<?>, BiFunction<String, String, ParticipantResource>> resourceLookupFunctions = new HashMap<>();

    @Override
    public ServiceResult<Void> authorize(SecurityContext securityContext, String resourceOwnerId, String resourceId, Class<? extends ParticipantResource> resourceClass) {
        var securityPrincipalName = securityContext.getUserPrincipal().getName();

        if (resourceOwnerId == null) {
            return ServiceResult.unauthorized("resourceOwnerId is mandatory but was null when querying for object with ID '%s' of type '%s'. Security Principal: '%s'".formatted(resourceId, resourceClass, securityPrincipalName));
        }

        var function = resourceLookupFunctions.get(resourceClass);
        if (function == null) {
            return ServiceResult.unauthorized("User access for '%s' to resource ID '%s' of type '%s' cannot be verified".formatted(securityPrincipalName, resourceId, resourceClass));
        }

        var resource = function.apply(resourceOwnerId, resourceId);
        if (resource == null) {
            return ServiceResult.notFound("No Resource of type '%s' with ID '%s' was found for owner '%s'.".formatted(resourceClass, resourceId, resourceOwnerId));
        }

        if (securityContext.isUserInRole(ParticipantPrincipal.ROLE_ADMIN)) {
            return ServiceResult.success();
        }

        // for all other users, the service principal, the resource owner and the participantContextID must be equal
        if (!Objects.equals(securityPrincipalName, resourceOwnerId) || !Objects.equals(resource.getParticipantContextId(), resourceOwnerId)) {
            return ServiceResult.unauthorized("User '%s' is not authorized to access this resource.".formatted(securityPrincipalName));
        }

        return ServiceResult.success();

    }

    @Override
    public void addLookupFunction(Class<?> resourceClass, BiFunction<String, String, ParticipantResource> lookupFunction) {
        resourceLookupFunctions.put(resourceClass, lookupFunction);
    }
}