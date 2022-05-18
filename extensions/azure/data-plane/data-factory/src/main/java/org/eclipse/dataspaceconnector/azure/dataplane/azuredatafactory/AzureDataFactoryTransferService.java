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

package org.eclipse.dataspaceconnector.azure.dataplane.azuredatafactory;

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.TransferService;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * {@link TransferService} implementation that performs transfers in Azure Data Factory.
 */
public class AzureDataFactoryTransferService implements TransferService {
    private final AzureDataFactoryTransferRequestValidator validator;
    private final AzureDataFactoryTransferManager transferManager;

    public AzureDataFactoryTransferService(AzureDataFactoryTransferRequestValidator validator, AzureDataFactoryTransferManager transferManager) {
        this.validator = validator;
        this.transferManager = transferManager;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return validator.canHandle(request);
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        return validator.validate(request);
    }

    @Override
    public CompletableFuture<StatusResult<Void>> transfer(DataFlowRequest request) {
        return transferManager.transfer(request);
    }
}
