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
     */
    @NotNull
    DataFlowInitiateResponse initiateFlow(DataRequest dataRequest);

}
