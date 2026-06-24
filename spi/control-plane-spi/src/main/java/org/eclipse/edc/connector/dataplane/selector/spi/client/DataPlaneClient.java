/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.selector.spi.client;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

/**
 * Main interaction interface for an EDC runtime (=control plane) to communicate with the DPF.
 */
@ExtensionPoint
public interface DataPlaneClient {

    /**
     * Prepare the data flow through provisioning
     *
     * @param request the request.
     * @return success if the data flow preparation has been triggered/executed, failure otherwise
     */
    StatusResult<DataFlowResponseMessage> prepare(DataFlowProvisionMessage request);

    /**
     * Delegates data transfer to the Data Plane.
     */
    StatusResult<DataFlowResponseMessage> start(DataFlowStartMessage request);

    /**
     * Suspend the transfer.
     *
     * @param transferProcessId the transfer process id.
     * @return success if the transfer has been suspended, failure otherwise.
     */
    StatusResult<Void> suspend(String transferProcessId);

    /**
     * Terminate the transfer.
     *
     * @param transferProcessId the transfer process id.
     * @return success if the transfer has been terminated, failure otherwise.
     */
    StatusResult<Void> terminate(String transferProcessId);

}
