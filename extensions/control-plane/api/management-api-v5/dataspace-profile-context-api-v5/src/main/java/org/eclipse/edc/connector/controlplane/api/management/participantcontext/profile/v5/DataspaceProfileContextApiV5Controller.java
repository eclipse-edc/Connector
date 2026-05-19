/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.controlplane.api.management.participantcontext.profile.v5;


import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.protocol.spi.AssociateDataspaceProfileContext;
import org.eclipse.edc.protocol.spi.ParticipantProfileService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.protocol.spi.AssociateDataspaceProfileContext.ASSOCIATE_DATASPACE_PROFILE_CONTEXT_TYPE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v5beta/participants/{participantContextId}/profiles")
public class DataspaceProfileContextApiV5Controller implements DataspaceProfileContextApiV5 {

    private final AuthorizationService authorizationService;
    private final ParticipantProfileService profileResolver;
    private final TypeTransformerRegistry transformerRegistry;
    private final Monitor monitor;

    public DataspaceProfileContextApiV5Controller(AuthorizationService authorizationService, ParticipantProfileService profileResolver,
                                                  TypeTransformerRegistry transformerRegistry, Monitor monitor) {
        this.authorizationService = authorizationService;
        this.profileResolver = profileResolver;
        this.transformerRegistry = transformerRegistry;
        this.monitor = monitor;
    }


    @GET
    @RolesAllowed({ParticipantPrincipal.ROLE_PROVISIONER, ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    @Override
    public JsonArray getProfilesV5(@PathParam("participantContextId") String participantContextId, @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, participantContextId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        var profiles = profileResolver.resolveAll(participantContextId);
        return profiles.stream()
                .map(profile -> transformerRegistry.transform(profile, JsonObject.class)
                        .onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());

    }

    @PUT
    @RolesAllowed({ParticipantPrincipal.ROLE_PROVISIONER, ParticipantPrincipal.ROLE_ADMIN})
    @RequiredScope("management-api:write")
    @Override
    public void associateProfiles(@PathParam("participantContextId") String participantContextId, @SchemaType(ASSOCIATE_DATASPACE_PROFILE_CONTEXT_TYPE_TERM) JsonObject request) {

        var context = transformerRegistry.transform(request, AssociateDataspaceProfileContext.class)
                .orElseThrow(InvalidRequestException::new);
        
        profileResolver.associateProfiles(participantContextId, context.profiles())
                .orElseThrow((f) -> new InvalidRequestException("Failed to associate profiles to ParticipantContext: %s".formatted(f.getFailureDetail())));
    }

}
