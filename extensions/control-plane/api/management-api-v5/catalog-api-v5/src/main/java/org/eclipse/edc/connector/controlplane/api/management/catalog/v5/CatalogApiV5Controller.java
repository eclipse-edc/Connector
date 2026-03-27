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

package org.eclipse.edc.connector.controlplane.api.management.catalog.v5;

import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest;
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.BadGatewayException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.validation.SchemaType;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequest.CATALOG_REQUEST_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest.DATASET_REQUEST_TYPE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v5alpha/participants/{participantContextId}/catalog")
public class CatalogApiV5Controller implements CatalogApiV5 {
    private final AuthorizationService authorizationService;
    private final CatalogService service;
    private final TypeTransformerRegistry transformerRegistry;
    private final ParticipantContextService participantContextService;

    public CatalogApiV5Controller(CatalogService service, TypeTransformerRegistry transformerRegistry,
                                  AuthorizationService authorizationService, ParticipantContextService participantContextService) {
        this.service = service;
        this.transformerRegistry = transformerRegistry;
        this.authorizationService = authorizationService;
        this.participantContextService = participantContextService;
    }


    @POST
    @Path("/request")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    @Override
    public void requestCatalogV5(@PathParam("participantContextId") String participantContextId,
                                 @SchemaType(CATALOG_REQUEST_TYPE_TERM) JsonObject requestBody,
                                 @Suspended AsyncResponse response,
                                 @Context SecurityContext securityContext) {

        var participantContext = preAuthorize(participantContextId, securityContext);

        var request = transformerRegistry.transform(requestBody, CatalogRequest.class)
                .orElseThrow(InvalidRequestException::new);

        var scopes = request.getAdditionalScopes().toArray(new String[0]);
        service.requestCatalog(participantContext, request.getCounterPartyId(), request.getCounterPartyAddress(), request.getProtocol(), request.getQuerySpec(), scopes)
                .whenComplete((result, throwable) -> {
                    try {
                        response.resume(toResponse(result, throwable));
                    } catch (Throwable mapped) {
                        response.resume(mapped);
                    }
                });
    }

    @POST
    @Path("/dataset/request")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    @Override
    public void getDatasetV5(@PathParam("participantContextId") String participantContextId,
                             @SchemaType(DATASET_REQUEST_TYPE_TERM) JsonObject requestBody,
                             @Suspended AsyncResponse response,
                             @Context SecurityContext securityContext) {

        var participantContext = preAuthorize(participantContextId, securityContext);

        var request = transformerRegistry.transform(requestBody, DatasetRequest.class)
                .orElseThrow(InvalidRequestException::new);

        service.requestDataset(participantContext, request.getId(), request.getCounterPartyId(), request.getCounterPartyAddress(), request.getProtocol())
                .whenComplete((result, throwable) -> {
                    try {
                        response.resume(toResponse(result, throwable));
                    } catch (Throwable mapped) {
                        response.resume(mapped);
                    }
                });
    }

    /**
     * utility method that performs authorization on the participant context with the given ID and return the {@link ParticipantContext}
     * throws an exception if the authorization fails. the exception does not need to be caught.
     */

    private ParticipantContext preAuthorize(String participantContextId, SecurityContext securityContext) {
        authorizationService.authorize(securityContext, participantContextId, participantContextId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        return participantContextService.getParticipantContext(participantContextId)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));
    }

    private byte[] toResponse(StatusResult<byte[]> result, Throwable throwable) throws Throwable {
        if (throwable == null) {
            if (result.succeeded()) {
                return result.getContent();
            } else {
                throw new BadGatewayException(result.getFailureDetail());
            }
        } else {
            if (throwable instanceof EdcException || throwable.getCause() instanceof EdcException) {
                throw new BadGatewayException(throwable.getMessage());
            } else {
                throw throwable;
            }
        }
    }

}
