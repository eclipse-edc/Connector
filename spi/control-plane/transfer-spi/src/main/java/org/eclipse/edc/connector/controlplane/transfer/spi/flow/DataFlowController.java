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

package org.eclipse.edc.connector.controlplane.transfer.spi.flow;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Handles a data flow.
 */
public interface DataFlowController {

    /**
     * Returns true if the manager can handle the Transfer Process.
     *
     * @param transferProcess the TransferProcess
     * @return true if it can handle the TransferProcess, false otherwise.
     */
    boolean canHandle(TransferProcess transferProcess);

    StatusResult<DataFlowResponse> provision(TransferProcess transferProcess, Policy policy);

    /**
     * Initiate a data flow.
     *
     * <p>Implementations should not throw exceptions. If an unexpected exception occurs and the flow should be re-attempted, set {@link ResponseStatus#ERROR_RETRY} in the
     * response. If an exception occurs and re-tries should not be re-attempted, set {@link ResponseStatus#FATAL_ERROR} in the response. </p>
     *
     * @param transferProcess the transfer process
     * @param policy          the contract agreement usage policy for the asset being transferred
     */
    @NotNull
    StatusResult<DataFlowResponse> start(TransferProcess transferProcess, Policy policy);

    /**
     * Suspend a data flow.
     *
     * @param transferProcess the transfer process.
     * @return success if the flow is suspended correctly, failure otherwise;
     */
    default StatusResult<Void> suspend(TransferProcess transferProcess) {
        return terminate(transferProcess);
    }

    /**
     * Terminate a data flow.
     *
     * @param transferProcess the transfer process.
     * @return success if the flow is terminated correctly, failure otherwise;
     */
    StatusResult<Void> terminate(TransferProcess transferProcess);

    /**
     * Returns transfer types that the controller can handle for the specified Asset.
     *
     * @return transfer type set.
     */
    Set<String> transferTypesFor(Asset asset);
}
