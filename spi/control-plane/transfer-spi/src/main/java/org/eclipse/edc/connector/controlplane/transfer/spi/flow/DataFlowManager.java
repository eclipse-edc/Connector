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
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.response.StatusResult;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Manages data flows and dispatches to {@link DataFlowController}s.
 * Priority is used to decide which controller should be chosen first, higher priority values will make the controller
 * being chosen first.
 */
@ExtensionPoint
public interface DataFlowManager {

    /**
     * Register the controller. The priority is set to 0.
     */
    void register(DataFlowController controller);

    /**
     * Register the controller with a specific priority.
     *
     * @param priority   the priority.
     * @param controller the controller.
     */
    void register(int priority, DataFlowController controller);

    /**
     * Prepare a consumer data flow.
     *
     * @param transferProcess the transfer process
     * @param policy          the contract agreement usage policy for the asset being transferred
     * @return succeeded StatusResult if preparation has been initiated/completed correctly, failed one otherwise.
     */
    @NotNull
    StatusResult<DataFlowResponse> prepare(TransferProcess transferProcess, Policy policy);

    /**
     * Starts a provider data flow.
     *
     * @param transferProcess the transfer process
     * @param policy          the contract agreement usage policy for the asset being transferred
     * @return succeeded StatusResult if flow has been initiated correctly, failed one otherwise.
     */
    @NotNull
    StatusResult<DataFlowResponse> start(TransferProcess transferProcess, Policy policy);

    /**
     * Terminates a data flow.
     *
     * @param transferProcess the transfer process.
     * @return success if the flow has been stopped correctly, failed otherwise.
     */
    @NotNull
    StatusResult<Void> terminate(TransferProcess transferProcess);

    /**
     * Suspend a transfer
     *
     * @param transferProcess the transfer process.
     * @return success if the transfer has been suspended correctly, failed otherwise.
     */
    @NotNull
    StatusResult<Void> suspend(TransferProcess transferProcess);

    /**
     * Returns the transfer types available for a specific asset.
     *
     * @param asset the asset.
     * @return transfer types list.
     */
    Set<String> transferTypesFor(Asset asset);

    /**
     * Returns the transfer types available for a specific asset given its id.
     *
     * @param assetId the asset id.
     * @return transfer types list.
     */
    Set<String> transferTypesFor(String assetId);
}
