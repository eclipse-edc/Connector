/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.api.controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.response.TransferErrorResponse;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import java.util.List;

import static jakarta.ws.rs.core.Response.status;
import static java.lang.String.format;

@Path("/transfer")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
public class DataPlaneControlApiController implements DataPlaneControlApi {
    private final DataPlaneManager dataPlaneManager;

    public DataPlaneControlApiController(DataPlaneManager dataPlaneManager) {
        this.dataPlaneManager = dataPlaneManager;
    }

    @POST
    @Override
    public void initiateTransfer(DataFlowStartMessage request, @Suspended AsyncResponse response) {
        // TODO token authentication
        var result = dataPlaneManager.validate(request);
        if (result.succeeded()) {
            dataPlaneManager.start(request);
            response.resume(Response.ok().build());
        } else {
            var resp = result.getFailureMessages().isEmpty() ?
                    badRequest(format("Failed to validate request: %s", request.getId())) :
                    badRequest(result.getFailureMessages());
            response.resume(resp);
        }
    }

    @GET
    @Override
    @Path("/{transferProcessId}")
    public DataFlowStates getTransferState(@PathParam("transferProcessId") String transferProcessId) {
        return dataPlaneManager.getTransferState(transferProcessId);
    }

    @DELETE
    @Path("/{transferProcessId}")
    @Override
    public void terminateTransfer(@PathParam("transferProcessId") String transferProcessId, @Suspended AsyncResponse response) {
        dataPlaneManager.terminate(transferProcessId)
                .onSuccess(r -> response.resume(Response.noContent().build()))
                .onFailure(f -> response.resume(badRequest(List.of("Cannot terminate transfer: " + f.getFailureDetail()))));
    }

    private Response badRequest(String error) {
        return badRequest(List.of(error));
    }

    private Response badRequest(List<String> errors) {
        return status(Response.Status.BAD_REQUEST).entity(new TransferErrorResponse(errors)).build();
    }
}
