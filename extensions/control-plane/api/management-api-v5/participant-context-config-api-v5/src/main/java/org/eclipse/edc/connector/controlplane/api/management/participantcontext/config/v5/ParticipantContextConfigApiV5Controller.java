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

package org.eclipse.edc.connector.controlplane.api.management.participantcontext.config.v5;


import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.service.ParticipantContextConfigService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration.PARTICIPANT_CONTEXT_CONFIG_TYPE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v5alpha/participants/{participantContextId}/config")
public class ParticipantContextConfigApiV5Controller implements ParticipantContextConfigApiV5 {

    private final ParticipantContextConfigService configService;
    private final TypeTransformerRegistry transformerRegistry;

    public ParticipantContextConfigApiV5Controller(ParticipantContextConfigService configService,
                                                   TypeTransformerRegistry transformerRegistry) {
        this.configService = configService;
        this.transformerRegistry = transformerRegistry;
    }

    @PUT
    @RolesAllowed({ParticipantPrincipal.ROLE_PROVISIONER})
    @RequiredScope("management-api:write")
    @Override
    public void setConfigV5(@PathParam("participantContextId") String participantContextId, @SchemaType(PARTICIPANT_CONTEXT_CONFIG_TYPE_TERM) JsonObject request) {

        var config = transformerRegistry.transform(request, ParticipantContextConfiguration.class)
                .orElseThrow(InvalidRequestException::new)
                .toBuilder()
                .participantContextId(participantContextId)
                .build();

        configService.save(config)
                .orElseThrow(exceptionMapper(ParticipantContextConfiguration.class, config.getParticipantContextId()));

    }

    @GET
    @RolesAllowed({ParticipantPrincipal.ROLE_PROVISIONER, ParticipantPrincipal.ROLE_ADMIN})
    @RequiredScope("management-api:read")
    @Override
    public JsonObject getConfigV5(@PathParam("participantContextId") String participantContextId, @Context SecurityContext securityContext) {

        var config = configService.get(participantContextId)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        return transformerRegistry.transform(config, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }
}
