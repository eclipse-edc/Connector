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
import org.eclipse.edc.connector.transfer.spi.types.command.CompleteTransferCommand;
import org.eclipse.edc.spi.command.EntityCommandHandler;

/**
 * Completes a transfer process and sends it to the {@link TransferProcessStates#COMPLETED} state.
 */
public class CompleteTransferCommandHandler extends EntityCommandHandler<CompleteTransferCommand, TransferProcess> {

    public CompleteTransferCommandHandler(TransferProcessStore store) {
        super(store);
    }

    @Override
    public Class<CompleteTransferCommand> getType() {
        return CompleteTransferCommand.class;
    }

    @Override
    protected boolean modify(TransferProcess process, CompleteTransferCommand command) {
        if (process.canBeCompleted()) {
            process.transitionCompleting();
            return true;
        }
        return false;
    }

}
