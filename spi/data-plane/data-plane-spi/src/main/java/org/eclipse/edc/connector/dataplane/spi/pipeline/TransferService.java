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
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

import java.util.concurrent.CompletableFuture;

/**
 * A service that can satisfy a {@link DataFlowRequest} by transferring data from a source to a destination.
 * This could be done in the Data-Plane internally, or it could leverage on external and more performant systems.
 */
public interface TransferService {

    /**
     * Returns true if this service can transfer the request.
     */
    boolean canHandle(DataFlowRequest request);

    /**
     * Returns a true result if the request is valid.
     */
    Result<Boolean> validate(DataFlowRequest request);

    /**
     * Transfers data from source to destination.
     */
    CompletableFuture<StreamResult<Object>> transfer(DataFlowRequest request);

    /**
     * Terminate a data flow.
     *
     * @param dataFlow the data flow.
     */
    StreamResult<Void> terminate(DataFlow dataFlow);

}
