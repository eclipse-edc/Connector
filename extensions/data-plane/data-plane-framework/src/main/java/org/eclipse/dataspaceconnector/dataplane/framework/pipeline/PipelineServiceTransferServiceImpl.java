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
package org.eclipse.dataspaceconnector.dataplane.framework.pipeline;

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.TransferService;
import org.eclipse.dataspaceconnector.dataplane.spi.result.TransferResult;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link TransferService} that performs transfers using a
 * {@link PipelineService}.
 */
public class PipelineServiceTransferServiceImpl implements TransferService {
    private final PipelineService pipelineService;

    public PipelineServiceTransferServiceImpl(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return pipelineService.canHandle(request);
    }

    @Override
    public Result<Boolean> validate(DataFlowRequest request) {
        return pipelineService.validate(request);
    }

    @Override
    public CompletableFuture<TransferResult> transfer(DataFlowRequest request) {
        return pipelineService.transfer(request);
    }
}
