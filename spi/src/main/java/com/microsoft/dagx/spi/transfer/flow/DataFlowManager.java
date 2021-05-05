/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.transfer.flow;

import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
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
     */
    @NotNull
    DataFlowInitiateResponse initiate(DataRequest dataRequest);
}
