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
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Implements TCK web hooks.
 */
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/negotiations")
public class TckWebhookController {

    private ContractNegotiationService negotiationService;

    public TckWebhookController(ContractNegotiationService negotiationService) {
        this.negotiationService = negotiationService;
    }

    @POST
    @Path("requests")
    public void startNegotiation(ContractNegotiationRequest request) {

        var contractOffer = ContractOffer.Builder.newInstance()
                .id(request.offerId())
                .assetId(request.offerId())
                .policy(Policy.Builder.newInstance().assigner(request.providerId()).build())
                .build();
        var contractRequest = ContractRequest.Builder.newInstance()
                .callbackAddresses(List.of(CallbackAddress.Builder.newInstance().uri(request.connectorAddress()).build()))
                .counterPartyAddress(request.connectorAddress())
                .contractOffer(contractOffer)
                .protocol("dataspace-protocol-http")
                .build();
        negotiationService.initiateNegotiation(contractRequest);
        System.out.println("Negotiation");
    }
}
