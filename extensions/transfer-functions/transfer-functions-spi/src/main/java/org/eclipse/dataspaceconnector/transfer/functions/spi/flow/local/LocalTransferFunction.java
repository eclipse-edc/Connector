/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.transfer.functions.spi.flow.local;

import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;

/**
 * An extension that initiates a data transfer. Instances can initiate a transfer directly (i.e. in the current process) or invoke a sidecar deployment or remote system.
 *
 * Care should be taken when implementing a local transfer function since execution is in the same process as the current system. Local functions are therefore not appropriate
 * for large data. Moreover, slow or long transfers may block execution of other work as local functions are invoked synchronously on the transfer process management thread.
 */
@FunctionalInterface
public interface LocalTransferFunction {

    enum Status {
        COMPLETE, IN_PROCESS, ERROR_RETRY, FATAL_ERROR
    }

    /**
     * Initiates the transfer.
     */
    ResponseStatus initiateFlow(DataRequest dataRequest);

    /**
     * Returns the status of the current transfer.
     */
    default Status isComplete(String id) {
        return Status.COMPLETE;
    }
}
