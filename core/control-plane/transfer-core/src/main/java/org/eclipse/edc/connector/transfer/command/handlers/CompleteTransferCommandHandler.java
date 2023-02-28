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

import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.types.domain.transfer.command.CompleteTransferCommand;

import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;

/**
 * Completes a transfer process and sends it to the {@link TransferProcessStates#COMPLETED} state.
 */
public class CompleteTransferCommandHandler extends SingleTransferProcessCommandHandler<CompleteTransferCommand> {

    private final TransferProcessObservable observable;

    public CompleteTransferCommandHandler(TransferProcessStore store, TransferProcessObservable observable) {
        super(store);
        this.observable = observable;
    }

    @Override
    public Class<CompleteTransferCommand> getType() {
        return CompleteTransferCommand.class;
    }

    @Override
    protected boolean modify(TransferProcess process, CompleteTransferCommand command) {
        if (process.getState() == STARTED.code()) {
            process.transitionCompleted();
            return true;
        }
        return false;
    }

    @Override
    protected void postAction(TransferProcess process) {
        observable.invokeForEach(l -> l.completed(process));
    }
}
