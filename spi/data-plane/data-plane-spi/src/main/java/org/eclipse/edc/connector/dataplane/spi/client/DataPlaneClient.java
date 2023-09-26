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

package org.eclipse.edc.connector.dataplane.spi.client;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

/**
 * Main interaction interface for an EDC runtime (=control plane) to communicate with the DPF.
 */
@ExtensionPoint
public interface DataPlaneClient {

    /**
     * Delegates data transfer to the Data Plane.
     */
    StatusResult<Void> transfer(DataFlowRequest request);

    /**
     * Terminate the transfer.
     *
     * @param transferProcessId the transfer process id.
     * @return success if the transfer has been terminated, failure otherwise.
     */
    StatusResult<Void> terminate(String transferProcessId);
}
