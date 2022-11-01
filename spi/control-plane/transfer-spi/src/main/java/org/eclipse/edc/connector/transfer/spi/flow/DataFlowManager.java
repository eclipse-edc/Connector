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

import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;

/**
 * Manages data flows and dispatches to {@link DataFlowController}s.
 */
@ExtensionPoint
public interface DataFlowManager {

    /**
     * Register the controller.
     */
    void register(DataFlowController controller);

    /**
     * Initiates a data flow.
     *
     * @param dataRequest    the data to transfer
     * @param contentAddress the address to resolve the asset contents. This may be the original asset address or an address resolving to generated content.
     * @param policy         the contract agreement usage policy for the asset being transferred
     */
    @NotNull
    StatusResult<Void> initiate(DataRequest dataRequest, DataAddress contentAddress, Policy policy);
}
