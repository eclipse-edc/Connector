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

package org.eclipse.edc.connector.dataplane.spi.pipeline;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import java.util.concurrent.CompletableFuture;

/**
 * A service that can satisfy a {@link DataFlowStartMessage} by transferring data from a source to a destination.
 * This could be done in the Data-Plane internally, or it could leverage on external and more performant systems.
 */
public interface TransferService {

    /**
     * Returns true if this service can transfer the request.
     */
    boolean canHandle(DataFlowStartMessage request);

    /**
     * Returns success if the request is valid. Failure otherwise
     */
    Result<Void> validate(DataFlowStartMessage request);

    /**
     * Transfers data from a source to a destination using the provided data flow request.
     *
     * @param request The data flow request containing the necessary information for the transfer.
     * @return A CompletableFuture wrapping a StreamResult indicating the success or failure of the transfer.
     */
    CompletableFuture<StreamResult<Object>> transfer(DataFlowStartMessage request);

    /**
     * Transfers data from a source to a destination using the provided data flow request and data sink.
     *
     * @param request The data flow request containing the necessary information for the transfer.
     * @param sink    The data sink that will receive the transferred data.
     * @return A CompletableFuture wrapping a StreamResult indicating the success or failure of the transfer.
     */
    CompletableFuture<StreamResult<Object>> transfer(DataFlowStartMessage request, DataSink sink);

    /**
     * Terminate a data flow.
     *
     * @param dataFlow the data flow.
     */
    StreamResult<Void> terminate(DataFlow dataFlow);


    /**
     * Close all the ongoing DataFlows
     */
    void closeAll();


}
