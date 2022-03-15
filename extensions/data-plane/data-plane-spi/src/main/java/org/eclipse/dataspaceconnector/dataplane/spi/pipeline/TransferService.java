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
package org.eclipse.dataspaceconnector.dataplane.spi.pipeline;

import org.eclipse.dataspaceconnector.dataplane.spi.result.TransferResult;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

import java.util.concurrent.CompletableFuture;

/**
 * A service that can satisfy a {@link DataFlowRequest} by transferring data from a source to a destination.
 */
public interface TransferService {

    /**
     * Returns true if this service can transfer the request.
     */
    boolean canHandle(DataFlowRequest request);

    /**
     * Returns true if the request is valid.
     */
    Result<Boolean> validate(DataFlowRequest request);

    /**
     * Transfers data from source to destination.
     */
    CompletableFuture<TransferResult> transfer(DataFlowRequest request);
}
