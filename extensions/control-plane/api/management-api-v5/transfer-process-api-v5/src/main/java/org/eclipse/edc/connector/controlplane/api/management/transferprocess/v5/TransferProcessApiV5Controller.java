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

package org.eclipse.edc.connector.controlplane.api.management.transferprocess.v5;

import jakarta.annotation.security.RolesAllowed;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
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
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.SuspendTransfer;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TerminateTransfer;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferState;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.ResumeTransferCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.SuspendTransferCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.TerminateTransferCommand;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.edc.web.spi.validation.SchemaType;

import java.util.Optional;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.lang.String.format;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.SuspendTransfer.SUSPEND_TRANSFER_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TerminateTransfer.TERMINATE_TRANSFER_TYPE_TERM;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_TYPE_TERM;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.filterByParticipantContextId;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE_TERM;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.mapToException;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v5alpha/participants/{participantContextId}/transferprocesses")
public class TransferProcessApiV5Controller implements TransferProcessApiV5 {

    private final Monitor monitor;
    private final AuthorizationService authorizationService;
    private final ParticipantContextService participantContextService;
    private final TransferProcessService service;
    private final TypeTransformerRegistry transformerRegistry;

    public TransferProcessApiV5Controller(Monitor monitor, AuthorizationService authorizationService, ParticipantContextService participantContextService, TransferProcessService service, TypeTransformerRegistry transformerRegistry) {
        this.monitor = monitor;
        this.authorizationService = authorizationService;
        this.participantContextService = participantContextService;
        this.service = service;
        this.transformerRegistry = transformerRegistry;
    }

    @POST
    @Path("request")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    @Override
    public JsonArray queryTransferProcessesV5(@PathParam("participantContextId") String participantContextId,
                                              @SchemaType(EDC_QUERY_SPEC_TYPE_TERM) JsonObject querySpecJson,
                                              @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, participantContextId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.none();
        } else {
            querySpec = transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        var query = querySpec.toBuilder()
                .filter(filterByParticipantContextId(participantContextId))
                .build();

        return service.search(query).orElseThrow(exceptionMapper(TransferProcess.class)).stream()
                .map(transferProcess -> transformerRegistry.transform(transferProcess, JsonObject.class)
                        .onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    @GET
    @Path("{id}")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    @Override
    public JsonObject getTransferProcessV5(@PathParam("participantContextId") String participantContextId,
                                           @PathParam("id") String id,
                                           @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, id, TransferProcess.class)
                .orElseThrow(exceptionMapper(TransferProcess.class, id));

        var transferProcess = service.findById(id);
        if (transferProcess == null) {
            throw new ObjectNotFoundException(TransferProcess.class, id);
        }

        return transformerRegistry.transform(transferProcess, JsonObject.class)
                .onFailure(f -> monitor.warning(f.getFailureDetail()))
                .orElseThrow(failure -> new ObjectNotFoundException(TransferProcess.class, id));
    }

    @GET
    @Path("/{id}/state")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:read")
    @Override
    public JsonObject getTransferProcessStateV5(@PathParam("participantContextId") String participantContextId,
                                                @PathParam("id") String id,
                                                @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, id, TransferProcess.class)
                .orElseThrow(exceptionMapper(TransferProcess.class, id));

        return Optional.of(id)
                .map(service::getState)
                .map(TransferState::new)
                .map(state -> transformerRegistry.transform(state, JsonObject.class)
                        .onFailure(f -> monitor.warning(f.getFailureDetail()))
                        .orElseThrow(failure -> new ObjectNotFoundException(TransferProcess.class, id)))
                .orElseThrow(() -> new ObjectNotFoundException(TransferProcess.class, id));
    }

    @POST
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:write")
    @Override
    public JsonObject initiateTransferProcessV5(@PathParam("participantContextId") String participantContextId,
                                                @SchemaType(TRANSFER_REQUEST_TYPE_TERM) JsonObject request,
                                                @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, participantContextId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        var transferRequest = transformerRegistry.transform(request, TransferRequest.class)
                .orElseThrow(InvalidRequestException::new);

        var participantContext = participantContextService.getParticipantContext(participantContextId)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        var createdTransfer = service.initiateTransfer(participantContext, transferRequest)
                .onSuccess(d -> monitor.debug(format("Transfer Process created %s", d.getId())))
                .orElseThrow(it -> mapToException(it, TransferProcess.class));

        var responseDto = IdResponse.Builder.newInstance()
                .id(createdTransfer.getId())
                .createdAt(createdTransfer.getCreatedAt())
                .build();

        return transformerRegistry.transform(responseDto, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @POST
    @Path("/{id}/terminate")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:write")
    @Override
    public void terminateTransferProcessV5(@PathParam("participantContextId") String participantContextId,
                                           @PathParam("id") String id,
                                           @SchemaType(TERMINATE_TRANSFER_TYPE_TERM) JsonObject requestBody,
                                           @Context SecurityContext securityContext) {


        authorizationService.authorize(securityContext, participantContextId, id, TransferProcess.class)
                .orElseThrow(exceptionMapper(TransferProcess.class, id));

        var terminateTransfer = transformerRegistry.transform(requestBody, TerminateTransfer.class)
                .orElseThrow(InvalidRequestException::new);

        service.terminate(new TerminateTransferCommand(id, terminateTransfer.reason()))
                .onSuccess(tp -> monitor.debug(format("Termination requested for TransferProcess with ID %s", id)))
                .orElseThrow(exceptionMapper(TransferProcess.class, id));
    }

    @POST
    @Path("/{id}/suspend")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:write")
    @Override
    public void suspendTransferProcessV5(@PathParam("participantContextId") String participantContextId,
                                         @PathParam("id") String id,
                                         @SchemaType(SUSPEND_TRANSFER_TYPE_TERM) JsonObject requestBody,
                                         @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, id, TransferProcess.class)
                .orElseThrow(exceptionMapper(TransferProcess.class, id));

        var suspendTransfer = transformerRegistry.transform(requestBody, SuspendTransfer.class)
                .orElseThrow(InvalidRequestException::new);

        service.suspend(new SuspendTransferCommand(id, suspendTransfer.reason()))
                .onSuccess(tp -> monitor.debug(format("Suspension requested for TransferProcess with ID %s", id)))
                .orElseThrow(exceptionMapper(TransferProcess.class, id));
    }

    @POST
    @Path("/{id}/resume")
    @RolesAllowed({ParticipantPrincipal.ROLE_ADMIN, ParticipantPrincipal.ROLE_PARTICIPANT})
    @RequiredScope("management-api:write")
    @Override
    public void resumeTransferProcessV5(@PathParam("participantContextId") String participantContextId,
                                        @PathParam("id") String id,
                                        @Context SecurityContext securityContext) {

        authorizationService.authorize(securityContext, participantContextId, id, TransferProcess.class)
                .orElseThrow(exceptionMapper(TransferProcess.class, id));

        service.resume(new ResumeTransferCommand(id))
                .onSuccess(tp -> monitor.debug(format("Resumption requested for TransferProcess with ID %s", id)))
                .orElseThrow(exceptionMapper(TransferProcess.class, id));
    }
}
