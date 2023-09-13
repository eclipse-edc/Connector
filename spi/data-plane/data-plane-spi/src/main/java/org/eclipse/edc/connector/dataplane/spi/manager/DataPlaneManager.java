/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.spi.manager;

import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

import java.util.concurrent.CompletableFuture;

/**
 * Manages the execution of data plane requests. Methods that return {@link StreamResult} from their futures can use that value to respond to different failure conditions.
 * For example:
 * <p>
 * <pre>
 * dataPlaneManager.transfer(sink, flowRequest).whenComplete((result, throwable) -&gt; {
 *      if (result != null &amp;&amp; result.failed()) {
 *          switch (result.reason()) {
 *              case NOT_FOUND:
 *                  // process
 *                  break;
 *              case NOT_AUTHORIZED:
 *                  // process
 *                  break;
 *              case GENERAL_ERROR:
 *                  // process
 *                  break;
 *              }
 *      } else if (throwable != null) {
 *          reportError(response, throwable);
 *      }
 * });
 * </pre>
 */
@ExtensionPoint
public interface DataPlaneManager {

    /**
     * Determines if the data flow request is valid and can be processed by this runtime.
     */
    Result<Boolean> validate(DataFlowRequest dataRequest);

    /**
     * Initiates a transfer for the data flow request. This method is non-blocking with respect to processing the request.
     */
    void initiate(DataFlowRequest dataRequest);

    /**
     * Performs a data transfer using the supplied data sink.
     */
    CompletableFuture<StreamResult<Void>> transfer(DataSink sink, DataFlowRequest request);

    /**
     * Returns the transfer state for the process.
     */
    DataFlowStates transferState(String processId);
}
