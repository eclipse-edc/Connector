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

package org.eclipse.edc.api.auth.spi;

import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.participantcontext.spi.types.AbstractParticipantResource;
import org.eclipse.edc.participantcontext.spi.types.ParticipantResource;
import org.eclipse.edc.spi.result.ServiceResult;

import java.security.Principal;
import java.util.function.BiFunction;

/**
 * This service takes a {@link Principal}, that is typically obtained from the {@link SecurityContext} of an incoming
 * HTTP request, and checks whether this principal is authorized to access a particular resource, identified by ID and by object class.
 */
public interface AuthorizationService {
    /**
     * Checks whether the principal is authorized to access a particular resource. The resource in question is returned
     * in the {@link ServiceResult} if the authorization was successful.
     *
     * @param securityContext The {@link SecurityContext} that was obtained during the authentication phase of the request.
     *                        This represents the user to whom the auth token belongs. Not null.
     * @param resourceOwnerId The ID of the resource owner. This is typically the ID of the participant context. Not null.
     * @param resourceId      The ID of the resource. The resource must be of type {@link AbstractParticipantResource}.
     * @param resourceClass   The concrete type of the resource.
     * @return success if authorized, containing the {@link ParticipantResource}, {@link ServiceResult#unauthorized(String)}
     *          if the principal is not authorized, or {@link ServiceResult#notFound(String)} if the resource owner does not own the resource.
     */
    ServiceResult<Void> authorize(SecurityContext securityContext, String resourceOwnerId, String resourceId, Class<? extends ParticipantResource> resourceClass);

    /**
     * Register a function that can look up a particular resource type by ID. Typically, every resource that should be protected with
     * authorization registers a lookup function for the type of resource.
     */
    void addLookupFunction(Class<?> resourceClass, BiFunction<String, String, ParticipantResource> checkFunction);
}