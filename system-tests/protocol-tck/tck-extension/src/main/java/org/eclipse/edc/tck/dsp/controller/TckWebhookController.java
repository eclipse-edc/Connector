/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.tck.dsp.controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Implements TCK web hooks.
 */
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/")
public class TckWebhookController {

    private final Monitor monitor;
    private final ContractNegotiationService negotiationService;
    private final TransferProcessService transferProcessService;


    public TckWebhookController(Monitor monitor, ContractNegotiationService negotiationService, TransferProcessService transferProcessService) {
        this.monitor = monitor;
        this.negotiationService = negotiationService;
        this.transferProcessService = transferProcessService;
    }

    @POST
    @Path("/negotiations/requests")
    public void startNegotiation(ContractNegotiationRequest request) {

        var contractOffer = ContractOffer.Builder.newInstance()
                .id(request.offerId())
                .assetId(request.offerId())
                .policy(Policy.Builder.newInstance().assigner(request.providerId())
                        .type(PolicyType.OFFER)
                        .permission(Permission.Builder.newInstance().action(Action.Builder.newInstance().type("http://www.w3.org/ns/odrl/2/use").build()).build())
                        .build())
                .build();
        var contractRequest = ContractRequest.Builder.newInstance()
                .callbackAddresses(List.of(CallbackAddress.Builder.newInstance().uri(request.connectorAddress()).build()))
                .counterPartyAddress(request.connectorAddress())
                .contractOffer(contractOffer)
                .protocol("dataspace-protocol-http:2025/1")
                .build();

        monitor.debug("Starting contract negotiation for [provider, address, offer, asset]: [%s, %s, %s, %s]".formatted(request.providerId(), request.connectorAddress(), request.offerId(), request.offerId()));

        negotiationService.initiateNegotiation(contractRequest);
    }

    @POST
    @Path("/transfers/requests")
    public void startTransfer(TransferProcessRequest request) {

        var transferRequest = TransferRequest.Builder.newInstance()
                .transferType(request.format())
                .counterPartyAddress(request.connectorAddress())
                .protocol("dataspace-protocol-http:2025/1")
                .contractId(request.agreementId())
                .build();

        monitor.debug("Starting transfer process for [provider, address, agreement, format]: [%s, %s, %s, %s]".formatted(request.providerId(), request.connectorAddress(), request.agreementId(), request.format()));
        transferProcessService.initiateTransfer(transferRequest).orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }
}
