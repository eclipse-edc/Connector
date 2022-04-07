/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.samples.sample042;

import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.TransferProcessCommand;

import java.time.Duration;

public class CheckTransferProcessTimeoutCommand extends TransferProcessCommand {

    private final int batchSize;
    private final TransferProcessStates targetState;
    private final Duration maxAge;

    public CheckTransferProcessTimeoutCommand(int batchSize, TransferProcessStates targetState, Duration maxAge) {
        this.batchSize = batchSize;
        this.targetState = targetState;
        this.maxAge = maxAge;
    }

    public int getBatchSize() {
        return batchSize;
    }


    public TransferProcessStates getTargetState() {
        return targetState;
    }

    public Duration getMaxAge() {
        return maxAge;
    }
}
