/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.command.handlers;

import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.transfer.spi.types.command.TerminateTransferCommand;
import org.eclipse.edc.spi.command.SingleEntityCommandHandler;

/**
 * Terminates a transfer process and puts it in the {@link TransferProcessStates#TERMINATING} state.
 */
public class TerminateTransferCommandHandler extends SingleEntityCommandHandler<TerminateTransferCommand, TransferProcess> {

    public TerminateTransferCommandHandler(TransferProcessStore store) {
        super(store);
    }

    @Override
    public Class<TerminateTransferCommand> getType() {
        return TerminateTransferCommand.class;
    }

    @Override
    protected boolean modify(TransferProcess process, TerminateTransferCommand command) {
        if (process.canBeTerminated()) {
            process.transitionTerminating(command.getReason());
            return true;
        }

        return false;
    }

}
