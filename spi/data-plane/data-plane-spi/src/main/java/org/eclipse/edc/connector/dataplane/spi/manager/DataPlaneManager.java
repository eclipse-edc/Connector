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
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.entity.StateEntityManager;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.Nullable;

/**
 * Manages the execution of data plane requests.
 */
@ExtensionPoint
public interface DataPlaneManager extends StateEntityManager {

    long DEFAULT_FLOW_LEASE_TIME = 500;
    int DEFAULT_FLOW_LEASE_FACTOR = 5;

    /**
     * Determines if the data flow request is valid and can be processed by this runtime.
     */
    Result<Void> validate(DataFlowStartMessage dataRequest);

    /**
     * Provision a data flow on the consumer side.
     *
     * @param request the request.
     * @return success if the provisioning went through, failed otherwise.
     */
    Result<DataFlowResponseMessage> provision(DataFlowProvisionMessage request);

    /**
     * Starts a transfer for the data flow request. This method is non-blocking with respect to processing the request.
     *
     * @param startMessage The {@link DataFlowStartMessage}
     * @return success with the {@link DataFlowResponseMessage} if the request was correctly processed, failure otherwise
     */
    Result<DataFlowResponseMessage> start(DataFlowStartMessage startMessage);

    /**
     * Returns the transfer state for the process.
     */
    DataFlowStates getTransferState(String processId);

    /**
     * Terminate the data flow.
     *
     * @param dataFlowId the data flow id.
     * @return success if data flow is terminated, failed otherwise.
     */
    default StatusResult<Void> terminate(String dataFlowId) {
        return terminate(dataFlowId, null);
    }

    /**
     * Suspend the data flow.
     *
     * @param dataFlowId the data flow id.
     * @return success if data flow is terminated, failed otherwise.
     */
    StatusResult<Void> suspend(String dataFlowId);

    /**
     * Terminate the data flow and specifies a reason.
     *
     * @param dataFlowId the data flow id.
     * @param reason     the reason for the termination. May be null.
     * @return success if data flow is terminated, failed otherwise.
     */
    StatusResult<Void> terminate(String dataFlowId, @Nullable String reason);

    /**
     * Restart flows
     *
     * @return success if succeeded, failure otherwise.
     */
    StatusResult<Void> restartFlows();
}
