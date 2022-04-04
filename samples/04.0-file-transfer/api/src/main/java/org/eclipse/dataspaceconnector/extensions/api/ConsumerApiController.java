/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.extensions.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.FATAL_ERROR;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class ConsumerApiController {

    private final Monitor monitor;
    private final TransferProcessManager processManager;
    private final ConsumerContractNegotiationManager consumerNegotiationManager;
    private final TransferProcessStore transferProcessStore;

    public ConsumerApiController(Monitor monitor, TransferProcessManager processManager,
                                 ConsumerContractNegotiationManager consumerNegotiationManager, TransferProcessStore transferProcessStore) {
        this.monitor = monitor;
        this.processManager = processManager;
        this.consumerNegotiationManager = consumerNegotiationManager;
        this.transferProcessStore = transferProcessStore;
    }

    @POST
    @Path("negotiation")
    public Response initiateNegotiation(@QueryParam("connectorAddress") String connectorAddress,
                                        ContractOffer contractOffer) {
        if (contractOffer == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        var contractOfferRequest = ContractOfferRequest.Builder.newInstance()
                .contractOffer(contractOffer)
                .protocol("ids-multipart")
                .connectorId("urn:connector:provider") // counter party id matching the address !!
                .connectorAddress(connectorAddress)
                .type(ContractOfferRequest.Type.INITIAL)
                .build();

        var result = consumerNegotiationManager.initiate(contractOfferRequest);
        if (result.fatalError()) {
            return Response.serverError().build();
        }

        return Response.ok(result.getContent().getId()).build();
    }

    @POST
    @Path("file/{filename}")
    public Response initiateTransfer(@PathParam("filename") String filename, @QueryParam("connectorAddress") String connectorAddress,
                                     @QueryParam("destination") String destinationPath, @QueryParam("contractId") String contractId) {

        monitor.info(format("Received request for file %s against provider %s", filename, connectorAddress));

        Objects.requireNonNull(filename, "filename");
        Objects.requireNonNull(connectorAddress, "connectorAddress");

        var dataRequest = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString()) //this is not relevant, thus can be random
                .connectorAddress(connectorAddress) //the address of the provider connector
                .protocol("ids-multipart")
                .connectorId("consumer")
                .assetId(filename)
                .dataDestination(DataAddress.Builder.newInstance()
                        .keyName("keyName")
                        .type("File") //the provider uses this to select the correct DataFlowController
                        .property("path", destinationPath) //where we want the file to be stored
                        .build())
                .managedResources(false) //we do not need any provisioning
                .contractId(contractId)
                .build();

        var result = processManager.initiateConsumerRequest(dataRequest);

        return result.failed() ? Response.status(400).build() : Response.ok(result.getContent()).build();
    }

    @GET
    @Path("transfer/{id}")
    public Response getTransferById(@PathParam("id") String id) {
        return Optional.ofNullable(transferProcessStore.find(id))
                .map(
                        v -> Response.ok(v).build()
                ).orElse(
                        Response.status(NOT_FOUND).build()
                );
    }
}
