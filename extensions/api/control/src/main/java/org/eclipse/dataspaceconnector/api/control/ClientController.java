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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.api.control.response.NegotiationStatusResponse;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.transfer.TransferInitiateResult;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;


@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/control")
public class ClientController {

    private final TransferProcessManager transferProcessManager;
    private final ConsumerContractNegotiationManager consumerNegotiationManager;
    private final ContractNegotiationStore contractNegotiationStore;

    public ClientController(
            @NotNull TransferProcessManager transferProcessManager,
            @NotNull ConsumerContractNegotiationManager consumerNegotiationManager,
            @NotNull ContractNegotiationStore contractNegotiationStore) {
        this.transferProcessManager = Objects.requireNonNull(transferProcessManager);
        this.consumerNegotiationManager = Objects.requireNonNull(consumerNegotiationManager);
        this.contractNegotiationStore = Objects.requireNonNull(contractNegotiationStore);
    }

    @POST
    @Path("transfer")
    public Response addTransfer(DataRequest dataRequest) {

        if (dataRequest == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        TransferInitiateResult response = transferProcessManager.initiateConsumerRequest(dataRequest);
        return Response.ok(response.getContent()).build();
    }

    @POST
    @Path("negotiation")
    public Response initiateNegotiation(ContractOfferRequest contractOffer) { // TODO allow to the idsWebhookAddress via parameter
        if (contractOffer == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        var result = consumerNegotiationManager.initiate(contractOffer);
        if (result.failed()) {
            return Response.serverError().build();
        }

        return Response.ok(result.getContent().getId()).build();
    }

    @GET
    @Path("negotiation/{id}")
    public Response getNegotiationById(@PathParam("id") String id) {
        var negotiation = contractNegotiationStore.find(id);

        if (negotiation == null) {
            return Response.status(404).build();
        }

        return Response.ok(negotiation).build();
    }
    
    @GET
    @Path("negotiation/{id}/state")
    public Response getNegotiationStateById(@PathParam("id") String id) {
        var negotiation = contractNegotiationStore.find(id);
    
        if (negotiation == null) {
            return Response.status(404).build();
        }
        
        return Response.ok(new NegotiationStatusResponse(negotiation)).build();
    }

    @GET
    @Path("agreement/{id}")
    public Response getAgreementById(@PathParam("id") String id) {
        var agreement = contractNegotiationStore.findContractAgreement(id);

        if (agreement == null) {
            return Response.status(404).build();
        }

        return Response.ok(agreement).build();
    }
}
