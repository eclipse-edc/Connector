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

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.system.Feature;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;

/**
 * Manages data transfer processes. Currently synchronous and asynchronous data transfers are supported.
 * <br/>
 * The {@link DataRequest#isSync()} flag indicates whether a data request should be processed synchronously or asynchronously.
 */
@Feature("edc:core:transfer:transferprocessmanager")
public interface TransferProcessManager {

    /**
     * Initiates a data transfer process on the consumer.
     */
    TransferInitiateResult initiateConsumerRequest(DataRequest dataRequest);

    /**
     * Initiates a data transfer process on the provider.
     */
    TransferInitiateResult initiateProviderRequest(DataRequest dataRequest);

    // TODO: will be substituted by the upcoming command queue introduction
    void transitionRequestAck(String processId);

    // TODO: will be substituted by the upcoming command queue introduction
    void transitionProvisioned(String processId);

    // TODO: will be substituted by the upcoming command queue introduction
    void transitionError(String processId, String detail);

    // TODO: will be substituted by the upcoming command queue introduction
    Result<TransferProcessStates> deprovision(String processId);

}
