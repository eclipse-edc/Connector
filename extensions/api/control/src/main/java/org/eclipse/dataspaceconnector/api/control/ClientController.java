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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.spi.transfer.TransferInitiateResponse;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;


@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/control")
public class ClientController {

    private final TransferProcessManager transferProcessManager;

    public ClientController(@NotNull TransferProcessManager transferProcessManager) {
        this.transferProcessManager = Objects.requireNonNull(transferProcessManager);
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
}
