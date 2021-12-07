/*
 *  Copyright (c) 2020, 2020-2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.transfer;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

/**
 * Manages data transfer processes.
 * <br/>
 * A data transfer processes transitions through a series of states, which allows the system to model both terminating and non-terminating (e.g. streaming) transfers. Transitions
 * occur asynchronously, since long-running processes such as resource provisioning may need to be completed before transitioning to a subsequent state. The permissible state
 * transitions are defined by {@link org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates}.
 * <br/>
 * The transfer manager performs continual iterations, which seek to advance the state of transfer processes, including recovery, in a FIFO state-based ordering.
 * Each iteration will seek to transition a set number of processes for each state to avoid situations where an excessive number of processes in one state block progress of
 * processes in other states.
 * <br/>
 * If no processes need to be transitioned, the transfer manager will wait according to the the defined {@link TransferWaitStrategy} before conducting the next iteration.
 * A wait strategy may implement a backoff scheme.
 */
public interface TransferProcessManager {

    /**
     * Initiates a data transfer process on the consumer.
     */
    TransferInitiateResult initiateConsumerRequest(DataRequest dataRequest);

    /**
     * Initiates a data transfer process on the provider.
     */
    TransferInitiateResult initiateProviderRequest(DataRequest dataRequest);

}
