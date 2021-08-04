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

package org.eclipse.dataspaceconnector.transfer.core.flow;

import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowInitiateResponse;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus.FATAL_ERROR;

/**
 * The default data flow manager.
 */
public class DataFlowManagerImpl implements DataFlowManager {
    private final List<DataFlowController> controllers = new ArrayList<>();

    @Override
    public void register(DataFlowController controller) {
        controllers.add(controller);
    }

    @Override
    public @NotNull DataFlowInitiateResponse initiate(DataRequest dataRequest) {
        DataFlowController executor = getExecutor(dataRequest);
        if (executor == null) {
            return new DataFlowInitiateResponse(FATAL_ERROR, "Unable to process data request. No data flow controller found: " + dataRequest.getId());
        }
        return executor.initiateFlow(dataRequest);
    }

    @Nullable
    private DataFlowController getExecutor(DataRequest dataRequest) {
        for (DataFlowController manager : controllers) {
            if (manager.canHandle(dataRequest)) {
                return manager;
            }
        }
        return null;
    }
}
