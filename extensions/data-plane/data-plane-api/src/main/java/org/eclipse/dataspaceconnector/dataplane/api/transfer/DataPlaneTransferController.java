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
package org.eclipse.dataspaceconnector.dataplane.api.transfer;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.dataplane.api.common.ResponseFunctions.validationError;
import static org.eclipse.dataspaceconnector.dataplane.api.common.ResponseFunctions.validationErrors;

/**
 * Handles incoming control requests for the data plane.
 */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/transfer")
public class DataPlaneTransferController {
    private DataPlaneManager dataPlaneManager;

    public DataPlaneTransferController(DataPlaneManager dataPlaneManager) {
        this.dataPlaneManager = dataPlaneManager;
    }

    /**
     * Initiates a data transfer for the given request. The transfer will be performed asynchronously.
     */
    @POST
    public Response initiateRequest(DataFlowRequest request) {
        // TODO token authentication
        var result = dataPlaneManager.validate(request);
        if (result.failed()) {
            return result.getFailureMessages().isEmpty() ? validationError(format("Invalid request: %s", request.getId())) : validationErrors(result.getFailureMessages());
        }
        dataPlaneManager.initiateTransfer(request);
        return Response.ok().build();
    }
}
