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
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.NotifyPreparedCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.NotifyStartedCommand;
import org.eclipse.edc.signaling.domain.DataFlowPrepareMessage;
import org.eclipse.edc.signaling.domain.DataFlowResponseMessage;
import org.eclipse.edc.signaling.domain.DataFlowStartMessage;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.mapToException;

@Path("/transfers")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class DataPlaneTransferApiController implements DataPlaneTransferApi {

    private final TransferProcessService transferProcessService;
    private final TypeTransformerRegistry typeTransformerRegistry;

    public DataPlaneTransferApiController(TransferProcessService transferProcessService, TypeTransformerRegistry typeTransformerRegistry) {
        this.transferProcessService = transferProcessService;
        this.typeTransformerRegistry = typeTransformerRegistry;
    }

    @Path("/{transferId}/dataflow/prepared")
    @POST
    @Override
    public Response prepared(@PathParam("transferId") String transferId, DataFlowResponseMessage message) {
        typeTransformerRegistry.transform(message, DataFlowResponse.class)
                .map(ServiceResult::success)
                .orElse(failure -> ServiceResult.badRequest(failure.getMessages()))
                .map(response -> new NotifyPreparedCommand(transferId, response.getDataAddress()))
                .compose(transferProcessService::notifyPrepared)
                .orElseThrow(f -> mapToException(f, DataFlowPrepareMessage.class));

        return Response.ok().build();
    }

    @Path("/{transferId}/dataflow/started")
    @POST
    @Override
    public Response started(@PathParam("transferId") String transferId, DataFlowResponseMessage message) {
        typeTransformerRegistry.transform(message, DataFlowResponse.class)
                .map(ServiceResult::success)
                .orElse(failure -> ServiceResult.badRequest(failure.getMessages()))
                .map(response -> new NotifyStartedCommand(transferId, response.getDataAddress()))
                .compose(transferProcessService::notifyStarted)
                .orElseThrow(f -> mapToException(f, DataFlowStartMessage.class));

        return Response.ok().build();
    }

    @Path("/{transferId}/dataflow/completed")
    @POST
    @Override
    public Response completed(@PathParam("transferId") String transferId) {
        transferProcessService.complete(transferId).orElseThrow(exceptionMapper(TransferProcess.class, transferId));
        return Response.ok().build();
    }

}
