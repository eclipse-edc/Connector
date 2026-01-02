/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.port.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Path("/transfers")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class DataPlaneTransferApiController implements DataPlaneTransferApi {

    private final TransferProcessService transferProcessService;

    public DataPlaneTransferApiController(TransferProcessService transferProcessService) {
        this.transferProcessService = transferProcessService;
    }

    @Path("/{transferId}/dataflow/completed")
    @POST
    @Override
    public Response completed(@PathParam("transferId") String transferId) {
        transferProcessService.complete(transferId).orElseThrow(exceptionMapper(TransferProcess.class, transferId));
        return Response.ok().build();
    }

}
