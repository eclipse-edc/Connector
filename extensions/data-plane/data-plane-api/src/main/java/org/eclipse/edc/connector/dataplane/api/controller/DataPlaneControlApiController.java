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
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

import static java.lang.String.format;
import static org.eclipse.edc.connector.dataplane.api.response.ResponseFunctions.validationError;
import static org.eclipse.edc.connector.dataplane.api.response.ResponseFunctions.validationErrors;

@Path("/transfer")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class DataPlaneControlApiController implements DataPlaneControlApi {
    private final DataPlaneManager dataPlaneManager;

    public DataPlaneControlApiController(DataPlaneManager dataPlaneManager) {
        this.dataPlaneManager = dataPlaneManager;
    }

    @POST
    @Override
    public void initiateTransfer(DataFlowRequest request, @Suspended AsyncResponse response) {
        // TODO token authentication
        var result = dataPlaneManager.validate(request);
        if (result.succeeded()) {
            dataPlaneManager.initiate(request);
            response.resume(Response.ok().build());
        } else {
            var resp = result.getFailureMessages().isEmpty() ?
                    validationError(format("Failed to validate request: %s", request.getId())) :
                    validationErrors(result.getFailureMessages());
            response.resume(resp);
        }
    }

    @GET
    @Override
    @Path("/{transferProcessId}")
    public DataFlowStates getTransferState(@PathParam("transferProcessId") String transferProcessId) {
        return dataPlaneManager.transferState(transferProcessId);
    }

    @DELETE
    @Path("/{transferProcessId}")
    @Override
    public void terminateTransfer(@PathParam("transferProcessId") String transferProcessId, @Suspended AsyncResponse response) {
        dataPlaneManager.terminate(transferProcessId)
                .onSuccess(r -> response.resume(Response.noContent().build()))
                .onFailure(f -> response.resume(validationError("Cannot terminate transfer: " + f.getFailureDetail())));
    }

}
