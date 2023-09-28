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
import org.eclipse.edc.connector.transfer.spi.types.command.StopTransferCommand;
import org.eclipse.edc.spi.command.EntityCommandHandler;

import java.util.List;

import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.SUSPENDING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATING;

/**
 * Transition a TransferProcess to the {@link TransferProcessStates#STOPPING} state.
 * Only possible on PROVIDER side.
 */
public class StopTransferCommandHandler extends EntityCommandHandler<StopTransferCommand, TransferProcess> {

    public StopTransferCommandHandler(TransferProcessStore store) {
        super(store);
    }

    @Override
    public Class<StopTransferCommand> getType() {
        return StopTransferCommand.class;
    }

    @Override
    protected boolean modify(TransferProcess process, StopTransferCommand command) {
        if (process.canBeStopped() && List.of(COMPLETING, TERMINATING, SUSPENDING).contains(command.getSubsequentState())) {
            process.transitionStopping(command.getReason(), command.getSubsequentState());
            return true;
        }
        return false;
    }

}
