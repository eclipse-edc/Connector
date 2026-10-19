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

package org.eclipse.edc.tck.dps.virtual;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Map;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.tck.dps.virtual.VirtualDpsTckExtension.DATASPACE_PROFILE;

/**
 * Webhook controller for the DPS TCK on the virtual runtime. Receives a trigger from the TCK (acting as data plane)
 * and initiates a real TransferProcess for the configured participant context, so that the task-based state machine
 * dispatches a {@code DataFlowPrepareMessage} to the TCK data plane.
 */
@Path("/")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class VirtualDpsTckWebhookController {

    private final TransferProcessService transferProcessService;
    private final ParticipantContextService participantContextService;
    private final String participantContextId;
    private final Monitor monitor;

    public VirtualDpsTckWebhookController(TransferProcessService transferProcessService, ParticipantContextService participantContextService,
                                          String participantContextId, Monitor monitor) {
        this.transferProcessService = transferProcessService;
        this.participantContextService = participantContextService;
        this.participantContextId = participantContextId;
        this.monitor = monitor;
    }

    @POST
    @Path("/dataflows/trigger")
    public Response triggerDataFlowPreparation(DataFlowPrepareTriggerRequest request) {
        monitor.debug("Received DPS trigger: processId=%s, agreementId=%s, dataPlaneUrl=%s"
                .formatted(request.processId(), request.agreementId(), request.dataPlaneUrl()));

        var participantContext = participantContextService.getParticipantContext(participantContextId)
                .orElseThrow(f -> new EdcException("Cannot get participant context: " + f.getFailureDetail()));

        return transferProcessService.initiateTransfer(participantContext, TransferRequest.Builder.newInstance()
                        .id(request.processId())
                        .contractId(request.agreementId())
                        .transferType("HttpData-PULL")
                        .counterPartyAddress(request.dspUrl())
                        .protocol(DATASPACE_PROFILE)
                        .build()
                )
                .map(transferProcess -> Map.of("id", transferProcess.getId()))
                .map(responseBody -> Response.ok().entity(responseBody).build())
                .onSuccess(i -> monitor.debug("TransferProcess %s initiated successfully for agreementId=%s"
                        .formatted(request.processId(), request.agreementId())))
                .orElseThrow(f -> new EdcException("Failed to initiate transfer: " + f.getFailureDetail()));
    }

}
