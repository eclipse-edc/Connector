/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.port.api.management.v5;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.AuthorizationProfile;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.signaling.domain.DataPlaneRegistrationMessage;

import java.util.Map;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Path("/v5beta/participants/{participantContextId}/dataplanes")
public class DataPlaneRegistrationApiV5Controller implements org.eclipse.edc.signaling.port.api.management.v5.DataPlaneRegistrationApiV5 {

    private final DataPlaneSelectorService dataPlaneSelectorService;
    private final AuthorizationService authorizationService;

    public DataPlaneRegistrationApiV5Controller(DataPlaneSelectorService dataPlaneSelectorService, AuthorizationService authorizationService) {
        this.dataPlaneSelectorService = dataPlaneSelectorService;
        this.authorizationService = authorizationService;
    }

    @PUT
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PROVISIONER, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:write")
    @Override
    public Response registerV5(@PathParam("participantContextId") String participantContextId,
                               DataPlaneRegistrationMessage registration,
                               @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, participantContextId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        var dataplaneInstance = DataPlaneInstance.Builder.newInstance()
                .id(registration.dataplaneId())
                .url(registration.endpoint())
                .allowedTransferType(registration.transferTypes())
                .authorizationProfile(toAuthorizationProfile(registration.authorization()))
                .participantContextId(participantContextId)
                .labels(registration.labels())
                .build();

        dataPlaneSelectorService.register(dataplaneInstance)
                .orElseThrow(exceptionMapper(DataPlaneInstance.class, registration.dataplaneId()));

        return Response.ok().build();
    }

    @DELETE
    @Path("/{dataplaneId}")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PROVISIONER, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:write")
    @Override
    public Response deleteV5(@PathParam("participantContextId") String participantContextId,
                             @PathParam("dataplaneId") String dataplaneId,
                             @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, dataplaneId, DataPlaneInstance.class)
                .orElseThrow(exceptionMapper(DataPlaneInstance.class, dataplaneId));

        dataPlaneSelectorService.delete(dataplaneId)
                .orElseThrow(exceptionMapper(DataPlaneInstance.class, dataplaneId));

        return Response.ok().build();
    }

    private AuthorizationProfile toAuthorizationProfile(Map<String, Object> authorization) {
        if (authorization == null) {
            return null;
        }

        return new AuthorizationProfile((String) authorization.get("type"), authorization);
    }

}
