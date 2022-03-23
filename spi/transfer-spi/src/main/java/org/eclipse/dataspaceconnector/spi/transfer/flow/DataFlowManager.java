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

package org.eclipse.dataspaceconnector.spi.transfer.flow;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

/**
 * Manages data flows and dispatches to {@link DataFlowController}s.
 */
public interface DataFlowManager {

    /**
     * Register the controller.
     */
    void register(DataFlowController controller);

    /**
     * Initiates a data flow.
     *
     * @param dataRequest the data to transfer
     * @param policy the contract agreement usage policy for the asset being transferred
     */
    @NotNull
    DataFlowInitiateResult initiate(DataRequest dataRequest, Policy policy);
}
