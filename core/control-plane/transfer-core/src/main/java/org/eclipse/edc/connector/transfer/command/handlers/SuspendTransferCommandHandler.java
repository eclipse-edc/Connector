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
import org.eclipse.edc.connector.transfer.spi.types.command.SuspendTransferCommand;
import org.eclipse.edc.spi.command.EntityCommandHandler;

/**
 * Terminates a transfer process and puts it in the {@link TransferProcessStates#SUSPENDING} state.
 */
public class SuspendTransferCommandHandler extends EntityCommandHandler<SuspendTransferCommand, TransferProcess> {

    public SuspendTransferCommandHandler(TransferProcessStore store) {
        super(store);
    }

    @Override
    public Class<SuspendTransferCommand> getType() {
        return SuspendTransferCommand.class;
    }

    @Override
    protected boolean modify(TransferProcess process, SuspendTransferCommand command) {
        if (process.canBeSuspended()) {
            process.transitionSuspending(command.getReason());
            return true;
        }

        return false;
    }

}
