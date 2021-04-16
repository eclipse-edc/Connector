package com.microsoft.dagx.spi.transfer;

import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;

/**
 * Manages data transfer processes.
 *
 * A data transfer processes transitions through a series of states, which allows the system to model both terminating and non-terminating (e.g. streaming) transfers. Transitions
 * occur asynchronously, since long-running processes such as resource provisioning may need to be completed before transitioning to a subsequent state. The permissible state
 * transitions are defined by {@link com.microsoft.dagx.spi.types.domain.transfer.TransferProcessStates}.
 *
 * The transfer manager performs continual iterations, which seek to advance the state of transfer processes, including recovery, in a FIFO state-based ordering.
 * Each iteration will seek to transition a set number of processes for each state to avoid situations where an excessive number of processes in one state block progress of
 * processes in other states.
 *
 * If no processes need to be transitioned, the transfer manager will wait according to the the defined {@link TransferWaitStrategy} before conducting the next iteration.
 * A wait strategy may implement a backoff scheme.
 */
public interface TransferProcessManager {

    /**
     * Initiates a data transfer process on the client.
     */
    TransferInitiateResponse initiateClientRequest(DataRequest dataRequest);

    /**
     * Initiates a data transfer process on the provider.
     */
    TransferInitiateResponse initiateProviderRequest(DataRequest dataRequest);

}
