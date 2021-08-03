/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.spi.transfer.flow;

import org.eclipse.edc.spi.types.domain.transfer.DataRequest;
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
     * {@link org.eclipse.edc.spi.transfer.response.ResponseStatus#ERROR_RETRY} in the response. If an exception occurs and re-tries should not be re-attempted, set
     * {@link org.eclipse.edc.spi.transfer.response.ResponseStatus#FATAL_ERROR} in the response.
     */
    @NotNull
    DataFlowInitiateResponse initiateFlow(DataRequest dataRequest);

}
