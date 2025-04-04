/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.client.transferprocess;

import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.CompleteProvisionCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.TerminateTransferCommand;
import org.eclipse.edc.connector.dataplane.spi.port.TransferProcessApiClient;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import java.util.function.Function;

public class EmbeddedTransferProcessHttpClient implements TransferProcessApiClient {

    private final TransferProcessService transferProcessService;

    public EmbeddedTransferProcessHttpClient(TransferProcessService transferProcessService) {
        this.transferProcessService = transferProcessService;
    }

    @Override
    public Result<Void> completed(DataFlowStartMessage request) {
        return transferProcessService.complete(request.getProcessId()).flatMap(toResult());
    }

    @Override
    public Result<Void> failed(DataFlowStartMessage request, String reason) {
        return transferProcessService.terminate(new TerminateTransferCommand(request.getProcessId(), reason)).flatMap(toResult());
    }

    @Override
    public Result<Void> provisioned(String id, DataAddress newAddress) {
        return transferProcessService.completeProvision(new CompleteProvisionCommand(id, newAddress)).flatMap(toResult());
    }

    private Function<ServiceResult<Void>, Result<Void>> toResult() {
        return it -> {
            if (it.succeeded()) {
                return Result.success();
            } else {
                return Result.failure(it.getFailureDetail());
            }
        };
    }
}
