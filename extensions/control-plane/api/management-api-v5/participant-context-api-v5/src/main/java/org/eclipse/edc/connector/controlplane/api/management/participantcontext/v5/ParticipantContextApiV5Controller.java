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

package org.eclipse.edc.connector.controlplane.api.management.participantcontext.v5;


import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ServiceResultHandler;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantContext.PARTICIPANT_CONTEXT_TYPE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v5alpha/participants")
public class ParticipantContextApiV5Controller implements ParticipantContextApiV5 {

    private final ParticipantContextService participantContextService;
    private final AuthorizationService authorizationService;
    private final TypeTransformerRegistry transformerRegistry;
    private final Monitor monitor;

    public ParticipantContextApiV5Controller(ParticipantContextService participantContextService, AuthorizationService authorizationService,
                                             TypeTransformerRegistry transformerRegistry, Monitor monitor) {
        this.authorizationService = authorizationService;
        this.participantContextService = participantContextService;
        this.transformerRegistry = transformerRegistry;
        this.monitor = monitor;
    }

    @POST
    @RolesAllowed({ParticipantPrincipal.ROLE_PROVISIONER})
    @RequiredScope("management-api:write")
    @Override
    public JsonObject createParticipantV5(@SchemaType(PARTICIPANT_CONTEXT_TYPE_TERM) JsonObject request) {

        var participantContext = transformerRegistry.transform(request, ParticipantContext.class)
                .orElseThrow(InvalidRequestException::new);

        var created = participantContextService.createParticipantContext(participantContext)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContext.getParticipantContextId()));

        var responseDto = IdResponse.Builder.newInstance()
                .id(created.getParticipantContextId())
                .createdAt(created.getCreatedAt())
                .build();

        return transformerRegistry.transform(responseDto, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @GET
    @Path("{id}")
    @RolesAllowed({ParticipantPrincipal.ROLE_PROVISIONER, ParticipantPrincipal.ROLE_ADMIN})
    @RequiredScope("management-api:read")
    @Override
    public JsonObject getParticipantV5(@PathParam("id") String id, @Context SecurityContext securityContext) {

        var participant = participantContextService.getParticipantContext(id)
                .orElseThrow(exceptionMapper(ParticipantContext.class, id));

        return transformerRegistry.transform(participant, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @PUT
    @Path("{id}")
    @RolesAllowed({ParticipantPrincipal.ROLE_PROVISIONER})
    @RequiredScope("management-api:write")
    @Override
    public void updateParticipantV5(@PathParam("id") String id, @SchemaType(PARTICIPANT_CONTEXT_TYPE_TERM) JsonObject request) {
        var participantContext = transformerRegistry.transform(request, ParticipantContext.class)
                .orElseThrow(InvalidRequestException::new);

        participantContextService.updateParticipantContext(participantContext)
                .orElseThrow(exceptionMapper(ParticipantContext.class, id));

    }

    @DELETE
    @Path("{id}")
    @RolesAllowed({ParticipantPrincipal.ROLE_PROVISIONER})
    @RequiredScope("management-api:write")
    @Override
    public void deleteParticipantV5(@PathParam("id") String participantContextId) {
        participantContextService.deleteParticipantContext(participantContextId)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));
    }

    @GET
    @RolesAllowed({ParticipantPrincipal.ROLE_PROVISIONER, ParticipantPrincipal.ROLE_ADMIN})
    @RequiredScope("management-api:read")
    @Override
    public JsonArray getAllParticipantsV5(@DefaultValue("0") @QueryParam("offset") Integer offset,
                                          @DefaultValue("50") @QueryParam("limit") Integer limit) {
        var query = QuerySpec.Builder.newInstance()
                .offset(offset)
                .limit(limit)
                .build();

        return participantContextService.search(query).orElseThrow(ServiceResultHandler.exceptionMapper(QuerySpec.class, null))
                .stream()
                .map(it -> transformerRegistry.transform(it, JsonObject.class))
                .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }
}
