/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Fraunhofer Institute for Software and Systems Engineering - add negotiation endpoint
 *
 */


package org.eclipse.dataspaceconnector.api.control;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResponse;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.TransferInitiateResponse;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;


@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/control")
public class ClientController {

    private final Monitor monitor;
    private final TransferProcessManager transferProcessManager;
    private final ConsumerContractNegotiationManager consumerNegotiationManager;

    public ClientController(
            @NotNull Monitor monitor,
            @NotNull TransferProcessManager transferProcessManager,
            @NotNull ConsumerContractNegotiationManager consumerNegotiationManager) {
        this.monitor = Objects.requireNonNull(monitor);
        this.transferProcessManager = Objects.requireNonNull(transferProcessManager);
        this.consumerNegotiationManager = Objects.requireNonNull(consumerNegotiationManager);
    }

    @POST
    @Path("transfer")
    public Response addTransfer(DataRequest dataRequest) {

        if (dataRequest == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        TransferInitiateResponse response = transferProcessManager.initiateConsumerRequest(dataRequest);
        return Response.ok(response.getId()).build();
    }

    @POST
    @Path("negotiation")
    public Response initiateNegotiation(ContractOfferRequest contractOffer) {
        if (contractOffer == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        var result = consumerNegotiationManager.initiate(contractOffer);
        if (result.getStatus() == NegotiationResponse.Status.FATAL_ERROR) {
            return Response.serverError().build();
        }

        return Response.ok(result.getContractNegotiation().getId()).build();
    }
}
