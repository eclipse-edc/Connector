/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.spi.port;


import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

/**
 * {@link TransferProcessApiClient} is an abstraction for talking with Control Plane, in this case for signaling back
 * that the transfer at Data Plane level has been completed or failed. Implementors should call the Transfer Process Manager
 * for altering the state of the Transfer Process with the chosen protocol/implementation. The default implementation will use the HTTP APIs
 * of the Control Plane.
 */
@ExtensionPoint
public interface TransferProcessApiClient {

    /**
     * Mark the TransferProcess referenced by {@link DataFlowStartMessage#getProcessId()} as completed
     *
     * @param request The completed {@link DataFlowStartMessage}
     * @return the result.
     */
    StatusResult<Void> completed(DataFlowStartMessage request);

    /**
     * Mark the TransferProcess referenced by {@link DataFlowStartMessage#getProcessId()} as failed
     *
     * @param request The failed {@link DataFlowStartMessage}
     * @return the result.
     */
    StatusResult<Void> failed(DataFlowStartMessage request, String reason);

    /**
     * Mark the TransferProcess as provisioned.
     *
     * @param dataFlow the data flow.
     * @return the result.
     */
    StatusResult<Void> provisioned(DataFlow dataFlow);

}
