/*
 *  Copyright (c) 2020 - 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - add provision scope
 *
 */

package org.eclipse.dataspaceconnector.spi.transfer;

import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.policy.PolicyScope;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.TransferProcessCommand;

/**
 * Manages data transfer processes. Currently synchronous and asynchronous data transfers are supported.
 */
public interface TransferProcessManager {
    
    @PolicyScope
    String PROVISION_SCOPE = "provision";

    /**
     * Initiates a data transfer process on the consumer.
     */
    StatusResult<String> initiateConsumerRequest(DataRequest dataRequest);

    /**
     * Initiates a data transfer process on the provider.
     */
    StatusResult<String> initiateProviderRequest(DataRequest dataRequest, ParticipantAgent consumerAgent);

    /**
     * Adds a single Command to the queue which is processed sequentially.
     */
    void enqueueCommand(TransferProcessCommand command);

}
