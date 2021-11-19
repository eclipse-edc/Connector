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
 *
 */


package org.eclipse.dataspaceconnector.api.control;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;


@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/control")
public class ClientController {

    private static final String CONSUMER_ID = "urn:connector:consumer";

    private final TransferProcessStore transferProcessStore;

    public ClientController(@NotNull TransferProcessStore transferProcessStore) {
        this.transferProcessStore = Objects.requireNonNull(transferProcessStore);
    }

    @POST
    @Path("transfer/{id}") // TODO extend params when requirements are clear. This will probably require the Id of an contract and the asset id. Plus maybe additional parameters for the data address properties
    public Response addTransfer(@PathParam("id") String id, @QueryParam("provider") String provider) {

        DataAddress dataAddress = DataAddress.Builder.newInstance()
                .type("TODO")
                .property("TODO", "TODO")
                .build();
        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .dataDestination(dataAddress)
                .connectorAddress(provider)
                .connectorId(CONSUMER_ID)
                .build();
        TransferProcess transferProcess = TransferProcess.Builder.newInstance()
                .id(id)
                .dataRequest(dataRequest)
                .build();

        transferProcessStore.create(transferProcess);
        return Response.ok(transferProcess).build();
    }
}
