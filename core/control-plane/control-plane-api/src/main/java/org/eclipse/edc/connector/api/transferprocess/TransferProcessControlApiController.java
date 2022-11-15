/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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

package org.eclipse.edc.connector.api.transferprocess;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.api.transferprocess.model.TransferProcessFailStateDto;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;

import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;


@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path(TransferProcessControlApiController.PATH)
public class TransferProcessControlApiController implements TransferProcessControlApi {

    public static final String PATH = "/transferprocess";
    private final TransferProcessService transferProcessService;


    public TransferProcessControlApiController(TransferProcessService transferProcessService) {
        this.transferProcessService = transferProcessService;
    }


    @POST
    @Path("/{processId}/complete")
    @Override
    public void complete(@PathParam("processId") String processId) {
        transferProcessService.complete(processId).orElseThrow(exceptionMapper(TransferProcess.class, processId));
    }

    @POST
    @Path("/{processId}/fail")
    @Override
    public void fail(@PathParam("processId") String processId, @NotNull @Valid TransferProcessFailStateDto request) {
        transferProcessService.fail(processId, request.getErrorMessage()).orElseThrow(exceptionMapper(TransferProcess.class, processId));
    }

}
