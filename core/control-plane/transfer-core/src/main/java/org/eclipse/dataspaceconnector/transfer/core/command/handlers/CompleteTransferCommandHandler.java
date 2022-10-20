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

package org.eclipse.dataspaceconnector.transfer.core.command.handlers;

import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessObservable;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.CompleteTransferCommand;

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
        if (process.getState() == TransferProcessStates.IN_PROGRESS.code()) {
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
