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

package org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.v5;

import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.auth.spi.ParticipantPrincipal;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.NegotiationState;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.TerminateNegotiation;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.edc.web.spi.validation.SchemaType;

import java.util.Optional;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.CONTRACT_REQUEST_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.TerminateNegotiation.TERMINATE_NEGOTIATION_TYPE_TERM;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.filterByParticipantContextId;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v5alpha/participants/{participantContextId}/contractnegotiations")
public class ContractNegotiationApiV5Controller implements ContractNegotiationApiV5 {
    private final ContractNegotiationService service;
    private final ParticipantContextService participantContextService;
    private final AuthorizationService authorizationService;
    private final TypeTransformerRegistry transformerRegistry;
    private final Monitor monitor;

    public ContractNegotiationApiV5Controller(ContractNegotiationService service, ParticipantContextService participantContextService, AuthorizationService authorizationService, TypeTransformerRegistry transformerRegistry, Monitor monitor) {
        this.service = service;
        this.participantContextService = participantContextService;
        this.authorizationService = authorizationService;
        this.transformerRegistry = transformerRegistry;
        this.monitor = monitor;
    }

    @POST
    @Path("/request")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    @Override
    public JsonArray queryNegotiationsV5(@PathParam("participantContextId") String participantContextId,
                                         @SchemaType(EDC_QUERY_SPEC_TYPE_TERM) JsonObject querySpecJson,
                                         @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, participantContextId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.Builder.newInstance().build();
        } else {
            querySpec = transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        var query = querySpec.toBuilder()
                .filter(filterByParticipantContextId(participantContextId))
                .build();

        return service.search(query).orElseThrow(exceptionMapper(ContractNegotiation.class, null)).stream()
                .map(it -> transformerRegistry.transform(it, JsonObject.class))
                .peek(this::logIfError)
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    @Override
    public JsonObject getNegotiationV5(@PathParam("participantContextId") String participantContextId,
                                       @PathParam("id") String id,
                                       @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, id, ContractNegotiation.class)
                .orElseThrow(exceptionMapper(ContractNegotiation.class, id));

        return Optional.of(id)
                .map(service::findbyId)
                .map(it -> transformerRegistry.transform(it, JsonObject.class))
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(ContractNegotiation.class, id));
    }

    @GET
    @Path("/{id}/state")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    @Override
    public JsonObject getNegotiationStateV5(@PathParam("participantContextId") String participantContextId,
                                            @PathParam("id") String id,
                                            @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, id, ContractNegotiation.class)
                .orElseThrow(exceptionMapper(ContractNegotiation.class, id));

        return Optional.of(id)
                .map(service::getState)
                .map(NegotiationState::new)
                .map(state -> transformerRegistry.transform(state, JsonObject.class))
                .orElseThrow(() -> new ObjectNotFoundException(ContractNegotiation.class, id))
                .orElseThrow(failure -> new EdcException(failure.getFailureDetail()));
    }

    @GET
    @Path("/{id}/agreement")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    @Override
    public JsonObject getAgreementForNegotiationV5(@PathParam("participantContextId") String participantContextId,
                                                   @PathParam("id") String negotiationId,
                                                   @Context SecurityContext securityContext) {
        authorizationService.authorize(securityContext, participantContextId, negotiationId, ContractNegotiation.class)
                .orElseThrow(exceptionMapper(ContractNegotiation.class, negotiationId));

        return Optional.of(negotiationId)
                .map(service::getForNegotiation)
                .map(it -> transformerRegistry.transform(it, JsonObject.class)
                        .orElseThrow(failure -> new EdcException(failure.getFailureDetail())))
                .orElseThrow(() -> new ObjectNotFoundException(ContractNegotiation.class, negotiationId));
    }

    @POST
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:write")
    @Override
    public JsonObject initiateContractNegotiationV5(@PathParam("participantContextId") String participantContextId,
                                                    @SchemaType(CONTRACT_REQUEST_TYPE_TERM) JsonObject requestObject,
                                                    @Context SecurityContext securityContext) {


        authorizationService.authorize(securityContext, participantContextId, participantContextId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        var contractRequest = transformerRegistry.transform(requestObject, ContractRequest.class)
                .orElseThrow(InvalidRequestException::new);

        var participantContext = participantContextService.getParticipantContext(participantContextId)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        return service.initiateNegotiation(participantContext, contractRequest)
                .map(cn -> IdResponse.Builder.newInstance().id(cn.getId()).createdAt(cn.getCreatedAt()).build())
                .compose(idResponse -> ServiceResult.from(transformerRegistry.transform(idResponse, JsonObject.class)))
                .orElseThrow(exceptionMapper(ContractNegotiation.class, null));

    }

    @POST
    @Path("/{id}/terminate")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:write")
    @Override
    public void terminateNegotiationV5(@PathParam("participantContextId") String participantContextId,
                                       @PathParam("id") String id,
                                       @SchemaType(TERMINATE_NEGOTIATION_TYPE_TERM) JsonObject terminateNegotiation,
                                       @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, id, ContractNegotiation.class)
                .orElseThrow(exceptionMapper(ContractNegotiation.class, id));

        var terminate = transformerRegistry.transform(terminateNegotiation, TerminateNegotiation.class)
                .orElseThrow(InvalidRequestException::new);

        service.terminate(new TerminateNegotiationCommand(id, terminate.reason())).orElseThrow(exceptionMapper(ContractNegotiation.class, id));
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:write")
    @Override
    public void deleteNegotiationV5(@PathParam("participantContextId") String participantContextId,
                                    @PathParam("id") String id,
                                    @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, id, ContractNegotiation.class)
                .orElseThrow(exceptionMapper(ContractNegotiation.class, id));

        service.delete(id).orElseThrow(exceptionMapper(ContractNegotiation.class, id));
    }

    private void logIfError(Result<?> result) {
        result.onFailure(f -> monitor.warning(f.getFailureDetail()));
    }
}
