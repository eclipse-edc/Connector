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

package org.eclipse.edc.protocol.dsp.transferprocess.http.api.v2025.virtual.controller;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferError;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferProcessRequestMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferSuspensionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.protocol.dsp.http.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.http.spi.message.GetDspRequest;
import org.eclipse.edc.protocol.dsp.http.spi.message.PostDspRequest;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.ParticipantProfileResolver;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.V_2025_1_VERSION;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_START_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.transferprocess.http.api.TransferProcessApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.transferprocess.http.api.TransferProcessApiPaths.TRANSFER_COMPLETION;
import static org.eclipse.edc.protocol.dsp.transferprocess.http.api.TransferProcessApiPaths.TRANSFER_INITIAL_REQUEST;
import static org.eclipse.edc.protocol.dsp.transferprocess.http.api.TransferProcessApiPaths.TRANSFER_START;
import static org.eclipse.edc.protocol.dsp.transferprocess.http.api.TransferProcessApiPaths.TRANSFER_SUSPENSION;
import static org.eclipse.edc.protocol.dsp.transferprocess.http.api.TransferProcessApiPaths.TRANSFER_TERMINATION;


/**
 * Versioned Transfer endpoint for 2025/1 protocol version. Path is scoped by participant context
 * id, profile id and DSP protocol version segment. The version segment dispatches to this
 * controller class; the profile determines the JSON-LD namespace and protocol string used to
 * dispatch the request. The profile's DSP version must match this controller's version.
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/{participantContextId}/{profileId}" + BASE_PATH)
public class DspTransferProcessApiController20251 {

    private final TransferProcessProtocolService protocolService;
    private final ParticipantContextService participantContextService;
    private final ParticipantProfileResolver profileResolver;
    private final DspRequestHandler dspRequestHandler;

    public DspTransferProcessApiController20251(TransferProcessProtocolService protocolService,
                                                ParticipantContextService participantContextService,
                                                ParticipantProfileResolver profileResolver,
                                                DspRequestHandler dspRequestHandler) {
        this.protocolService = protocolService;
        this.participantContextService = participantContextService;
        this.profileResolver = profileResolver;
        this.dspRequestHandler = dspRequestHandler;
    }

    @GET
    @Path("/{id}")
    public Response getTransferProcess(@PathParam("participantContextId") String participantContextId,
                                       @PathParam("profileId") String profileId,
                                       @PathParam("id") String id, @HeaderParam(AUTHORIZATION) String token) {
        var profile = resolveProfile(participantContextId, profileId);
        var protocol = profile.name();
        var message = TransferProcessRequestMessage.Builder.newInstance()
                .protocol(protocol)
                .transferProcessId(id)
                .build();

        var request = GetDspRequest.Builder.newInstance(TransferProcessRequestMessage.class, TransferProcess.class, TransferError.class)
                .id(id)
                .token(token)
                .message(message)
                .serviceCall(protocolService::findById)
                .protocol(protocol)
                .errorProvider(TransferError.Builder::newInstance)
                .participantContextProvider(participantContextSupplier(participantContextId))
                .build();

        return dspRequestHandler.getResource(request);
    }

    @POST
    @Path(TRANSFER_INITIAL_REQUEST)
    public Response initiateTransferProcess(@PathParam("participantContextId") String participantContextId,
                                            @PathParam("profileId") String profileId,
                                            JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token) {
        var profile = resolveProfile(participantContextId, profileId);
        var request = PostDspRequest.Builder.newInstance(TransferRequestMessage.class, TransferProcess.class, TransferError.class)
                .message(jsonObject)
                .token(token)
                .expectedMessageType(profile.protocolNamespace().toIri(DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM))
                .serviceCall(protocolService::notifyRequested)
                .errorProvider(TransferError.Builder::newInstance)
                .protocol(profile.name())
                .participantContextProvider(participantContextSupplier(participantContextId))
                .build();

        return dspRequestHandler.createResource(request);
    }

    @POST
    @Path("{id}" + TRANSFER_START)
    public Response transferProcessStart(@PathParam("participantContextId") String participantContextId,
                                         @PathParam("profileId") String profileId,
                                         @PathParam("id") String id, JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token) {
        var profile = resolveProfile(participantContextId, profileId);
        var request = PostDspRequest.Builder.newInstance(TransferStartMessage.class, TransferProcess.class, TransferError.class)
                .processId(id)
                .expectedMessageType(profile.protocolNamespace().toIri(DSPACE_TYPE_TRANSFER_START_MESSAGE_TERM))
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyStarted)
                .errorProvider(TransferError.Builder::newInstance)
                .protocol(profile.name())
                .participantContextProvider(participantContextSupplier(participantContextId))
                .build();

        return dspRequestHandler.updateResource(request);
    }

    @POST
    @Path("{id}" + TRANSFER_COMPLETION)
    public Response transferProcessCompletion(@PathParam("participantContextId") String participantContextId,
                                              @PathParam("profileId") String profileId,
                                              @PathParam("id") String id, JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token) {
        var profile = resolveProfile(participantContextId, profileId);
        var request = PostDspRequest.Builder.newInstance(TransferCompletionMessage.class, TransferProcess.class, TransferError.class)
                .processId(id)
                .expectedMessageType(profile.protocolNamespace().toIri(DSPACE_TYPE_TRANSFER_COMPLETION_MESSAGE_TERM))
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyCompleted)
                .errorProvider(TransferError.Builder::newInstance)
                .protocol(profile.name())
                .participantContextProvider(participantContextSupplier(participantContextId))
                .build();

        return dspRequestHandler.updateResource(request);
    }

    @POST
    @Path("{id}" + TRANSFER_TERMINATION)
    public Response transferProcessTermination(@PathParam("participantContextId") String participantContextId,
                                               @PathParam("profileId") String profileId,
                                               @PathParam("id") String id, JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token) {
        var profile = resolveProfile(participantContextId, profileId);
        var request = PostDspRequest.Builder.newInstance(TransferTerminationMessage.class, TransferProcess.class, TransferError.class)
                .processId(id)
                .expectedMessageType(profile.protocolNamespace().toIri(DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE_TERM))
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifyTerminated)
                .errorProvider(TransferError.Builder::newInstance)
                .protocol(profile.name())
                .participantContextProvider(participantContextSupplier(participantContextId))
                .build();

        return dspRequestHandler.updateResource(request);
    }

    @POST
    @Path("{id}" + TRANSFER_SUSPENSION)
    public Response transferProcessSuspension(@PathParam("participantContextId") String participantContextId,
                                              @PathParam("profileId") String profileId,
                                              @PathParam("id") String id, JsonObject jsonObject, @HeaderParam(AUTHORIZATION) String token) {
        var profile = resolveProfile(participantContextId, profileId);
        var request = PostDspRequest.Builder.newInstance(TransferSuspensionMessage.class, TransferProcess.class, TransferError.class)
                .processId(id)
                .expectedMessageType(profile.protocolNamespace().toIri(DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE_TERM))
                .message(jsonObject)
                .token(token)
                .serviceCall(protocolService::notifySuspended)
                .errorProvider(TransferError.Builder::newInstance)
                .protocol(profile.name())
                .participantContextProvider(participantContextSupplier(participantContextId))
                .build();

        return dspRequestHandler.updateResource(request);
    }

    private DataspaceProfileContext resolveProfile(String participantContextId, String profileId) {
        var profile = profileResolver.resolve(participantContextId, profileId)
                .orElseThrow(() -> new NotFoundException("No profile '%s' for participant '%s'".formatted(profileId, participantContextId)));
        if (!V_2025_1_VERSION.equals(profile.protocolVersion().version())) {
            throw new NotFoundException("Profile '%s' is not for DSP version %s".formatted(profileId, V_2025_1_VERSION));
        }
        return profile;
    }

    private ParticipantContextSupplier participantContextSupplier(String id) {
        return () -> participantContextService.getParticipantContext(id);
    }

}
