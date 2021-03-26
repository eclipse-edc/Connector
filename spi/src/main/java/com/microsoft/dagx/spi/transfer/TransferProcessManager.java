package com.microsoft.dagx.spi.transfer;

import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;

/**
 * Manages data transfer processes.
 *
 * A data transfer processes transitions through a series of states, which allows the system to model both terminating and non-terminating (e.g. streaming) transfers. Transitions
 * occur asynchronously, since long-running processes such as resource provisioning may need to be completed before transitioning to a subsequent state.
 */
public interface TransferProcessManager {

    /**
     * Initiates a data transfer process.
     */
    TransferInitiateResponse initiate(DataRequest dataRequest);

}
