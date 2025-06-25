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

package org.eclipse.edc.connector.controlplane.api.transferprocess;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.controlplane.api.transferprocess.model.TransferProcessFailStateDto;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.CompleteProvisionCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.TerminateTransferCommand;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;


@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path(TransferProcessControlApiController.PATH)
public class TransferProcessControlApiController implements TransferProcessControlApi {

    public static final String PATH = "/transferprocess";
    private final TransferProcessService transferProcessService;
    private final Validator<TransferProcessFailStateDto> validator = new TransferProcessFailStateDtoValidator();

    public TransferProcessControlApiController(TransferProcessService transferProcessService) {
        this.transferProcessService = transferProcessService;
    }

    @POST
    @Path("/{processId}/complete")
    @Override
    public void complete(@PathParam("processId") String processId) {
        transferProcessService.complete(processId)
                .orElseThrow(exceptionMapper(TransferProcess.class, processId));
    }

    @POST
    @Path("/{processId}/fail")
    @Override
    public void fail(@PathParam("processId") String processId, TransferProcessFailStateDto request) {
        validator.validate(request).orElseThrow(ValidationFailureException::new);

        transferProcessService.terminate(new TerminateTransferCommand(processId, request.getErrorMessage()))
                .orElseThrow(exceptionMapper(TransferProcess.class, processId));
    }

    @POST
    @Path("/{processId}/provisioned")
    @Override
    public void provisioned(@PathParam("processId") String processId, DataAddress newDataAddress) {
        var command = new CompleteProvisionCommand(processId, newDataAddress);

        transferProcessService.completeProvision(command)
                .orElseThrow(exceptionMapper(TransferProcess.class, processId));
    }

    private static class TransferProcessFailStateDtoValidator implements Validator<TransferProcessFailStateDto> {
        @Override
        public ValidationResult validate(TransferProcessFailStateDto input) {
            if (input == null) {
                return ValidationResult.failure(Violation.violation("requestBody cannot be null", ""));
            }

            if (input.getErrorMessage() == null) {
                return ValidationResult.failure(Violation.violation("errorMessage cannot be null", "errorMessage"));
            }
            return ValidationResult.success();
        }
    }
}
