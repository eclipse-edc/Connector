/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.connector.transfer.spi.flow;

import org.eclipse.edc.connector.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;

/**
 * Handles a data flow.
 */
public interface DataFlowController {

    /**
     * Returns true if the manager can handle the Transfer Process.
     *
     * @param transferProcess    the TransferProcess
     * @return true if it can handle the TransferProcess, false otherwise.
     */
    boolean canHandle(TransferProcess transferProcess);

    /**
     * Initiate a data flow.
     *
     * <p>Implementations should not throw exceptions. If an unexpected exception occurs and the flow should be re-attempted, set {@link ResponseStatus#ERROR_RETRY} in the
     * response. If an exception occurs and re-tries should not be re-attempted, set {@link ResponseStatus#FATAL_ERROR} in the response. </p>
     *
     * @param transferProcess    the transfer process
     * @param policy             the contract agreement usage policy for the asset being transferred
     */
    @NotNull
    StatusResult<DataFlowResponse> initiateFlow(TransferProcess transferProcess, Policy policy);

    /**
     * Terminate a data flow.
     *
     * @param transferProcess the transfer process.
     * @return success if the flow is terminated correctly, failure otherwise;
     */
    StatusResult<Void> terminate(TransferProcess transferProcess);

    /**
     * Returns true if the manager can handle the data type.
     *
     * @param dataRequest    the request
     * @param contentAddress the address to resolve the asset contents. This may be the original asset address or an address resolving to generated content.
     * @deprecated please use {@link #canHandle(TransferProcess)}
     */
    @Deprecated(since = "0.2.1", forRemoval = true)
    default boolean canHandle(DataRequest dataRequest, DataAddress contentAddress) {
        return canHandle(TransferProcess.Builder.newInstance().dataRequest(dataRequest).contentDataAddress(contentAddress).build());
    }

    /**
     * Initiate a data flow.
     *
     * <p>Implementations should not throw exceptions. If an unexpected exception occurs and the flow should be re-attempted, set {@link ResponseStatus#ERROR_RETRY} in the
     * response. If an exception occurs and re-tries should not be re-attempted, set {@link ResponseStatus#FATAL_ERROR} in the response. </p>
     *
     * @param dataRequest    the request
     * @param contentAddress the address to resolve the asset contents. This may be the original asset address or an address resolving to generated content.
     * @param policy         the contract agreement usage policy for the asset being transferred
     * @deprecated please use {@link #initiateFlow(TransferProcess, Policy)}
     */
    @NotNull
    @Deprecated(since = "0.2.1", forRemoval = true)
    default StatusResult<DataFlowResponse> initiateFlow(DataRequest dataRequest, DataAddress contentAddress, Policy policy) {
        return initiateFlow(TransferProcess.Builder.newInstance().dataRequest(dataRequest).contentDataAddress(contentAddress).build(), policy);
    }

}
