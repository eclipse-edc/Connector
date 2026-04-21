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

package org.eclipse.edc.tck.dps.signaling;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Webhook controller for DPS TCK tests. Receives a trigger from the TCK (acting as data plane),
 * registers the TCK as the data plane instance, and initiates a real TransferProcess so the
 * connector's internal state machine dispatches a {@code DataFlowPrepareMessage} to the TCK.
 */
@Path("/")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class DpsTckWebhookController {

    private final TransferProcessService transferProcessService;
    private final SingleParticipantContextSupplier participantContextSupplier;
    private final Monitor monitor;

    public DpsTckWebhookController(TransferProcessService transferProcessService,
                                   SingleParticipantContextSupplier participantContextSupplier,
                                   Monitor monitor) {
        this.transferProcessService = transferProcessService;
        this.participantContextSupplier = participantContextSupplier;
        this.monitor = monitor;
    }

    @POST
    @Path("/dataflows/trigger")
    public Response triggerDataFlowPreparation(DataFlowPrepareTriggerRequest request) {
        monitor.debug("Received DPS trigger: processId=%s, agreementId=%s, dataPlaneUrl=%s"
                .formatted(request.processId(), request.agreementId(), request.dataPlaneUrl()));

        var participantContext = participantContextSupplier.get()
                .orElseThrow(f -> new EdcException("Cannot get participant context: " + f.getFailureDetail()));

        // Initiate the transfer: the state machine will pick it up in INITIAL state and call
        // DataFlowController.prepare(), which sends DataFlowPrepareMessage to the registered data plane
        var transferRequest = TransferRequest.Builder.newInstance()
                .id(request.processId())
                .contractId(request.agreementId())
                .transferType("HttpData-PULL")
                .counterPartyAddress(request.dspUrl())
                .protocol("dataspace-protocol-http:2025-1")
                .build();

        transferProcessService.initiateTransfer(participantContext, transferRequest)
                .orElseThrow(f -> new EdcException("Failed to initiate transfer: " + f.getFailureDetail()));

        monitor.debug("TransferProcess initiated successfully for agreementId=%s".formatted(request.agreementId()));
        return Response.ok().build();
    }

}
