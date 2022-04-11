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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

import java.util.Objects;
import java.util.UUID;

import static java.lang.String.format;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class ConsumerApiController {

    private final Monitor monitor;
    private final TransferProcessManager processManager;

    public ConsumerApiController(Monitor monitor, TransferProcessManager processManager) {
        this.monitor = monitor;
        this.processManager = processManager;
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

}
