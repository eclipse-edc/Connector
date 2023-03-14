/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - refactored
 *
 */

package org.eclipse.edc.connector.transfer.command.handlers;

import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.transfer.spi.types.command.CancelTransferCommand;

/**
 * Cancels a transfer process and puts it in the {@link TransferProcessStates#TERMINATED} state.
 *
 * @deprecated superseded by {@link TerminateTransferCommandHandler}
 */
@Deprecated(since = "milestone9")
public class CancelTransferCommandHandler extends SingleTransferProcessCommandHandler<CancelTransferCommand> {

    public CancelTransferCommandHandler(TransferProcessStore store) {
        super(store);
    }

    @Override
    public Class<CancelTransferCommand> getType() {
        return CancelTransferCommand.class;
    }

    @Override
    protected boolean modify(TransferProcess process, CancelTransferCommand command) {
        if (!process.canBeTerminated()) {
            return false;
        }
        process.transitionTerminating("transfer cancelled");
        return true;
    }

    @Override
    protected void postAction(TransferProcess process) {
    }
}
