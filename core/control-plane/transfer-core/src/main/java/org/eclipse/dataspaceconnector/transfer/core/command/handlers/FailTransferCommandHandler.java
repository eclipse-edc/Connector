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
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.FailTransferCommand;

/**
 * Fails a transfer process and sends it to the {@link TransferProcessStates#ERROR} state.
 */
public class FailTransferCommandHandler extends SingleTransferProcessCommandHandler<FailTransferCommand> {

    private final TransferProcessObservable observable;

    public FailTransferCommandHandler(TransferProcessStore store, TransferProcessObservable observable) {
        super(store);
        this.observable = observable;
    }

    @Override
    public Class<FailTransferCommand> getType() {
        return FailTransferCommand.class;
    }

    @Override
    protected boolean modify(TransferProcess process, FailTransferCommand command) {
        if (process.getState() == TransferProcessStates.IN_PROGRESS.code()) {
            process.transitionError(command.getErrorMessage());
            return true;
        }
        return false;
    }


    @Override
    protected void postAction(TransferProcess process) {
        observable.invokeForEach(l -> l.failed(process));
    }
}
