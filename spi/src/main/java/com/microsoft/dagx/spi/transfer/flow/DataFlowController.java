/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.transfer.flow;

import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

/**
 * Handles a data flow.
 */
public interface DataFlowController {

    /**
     * Returns true if the manager can handle the data type.
     */
    boolean canHandle(DataRequest dataRequest);

    /**
     * Initiate a data flow.
     *
     * Implementations should not throw exceptions. If an unexpected exception occurs and the flow should be re-attempted, set
     * {@link com.microsoft.dagx.spi.transfer.response.ResponseStatus#ERROR_RETRY} in the response. If an exception occurs and re-tries should not be re-attempted, set
     * {@link com.microsoft.dagx.spi.transfer.response.ResponseStatus#FATAL_ERROR} in the response.
     */
    @NotNull
    DataFlowInitiateResponse initiateFlow(DataRequest dataRequest);

}
