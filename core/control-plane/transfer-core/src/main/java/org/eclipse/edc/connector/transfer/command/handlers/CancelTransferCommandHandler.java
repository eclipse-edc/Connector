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
import org.eclipse.edc.connector.transfer.spi.types.command.CancelTransferCommand;

/**
 * Cancels a transfer process and sends it to the {@link TransferProcessStates#ERROR} state.
 */
public class CancelTransferCommandHandler extends SingleTransferProcessCommandHandler<CancelTransferCommand> {

    private final TransferProcessObservable observable;

    public CancelTransferCommandHandler(TransferProcessStore store, TransferProcessObservable observable) {
        super(store);
        this.observable = observable;
    }

    @Override
    public Class<CancelTransferCommand> getType() {
        return CancelTransferCommand.class;
    }

    @Override
    protected boolean modify(TransferProcess process, CancelTransferCommand command) {
        var state = process.getState();
        if (state == TransferProcessStates.COMPLETED.code() ||
                state == TransferProcessStates.ERROR.code() ||
                state == TransferProcessStates.CANCELLED.code() ||
                state == TransferProcessStates.TERMINATED.code()) {
            return false;
        }
        process.transitionCancelled();
        return true;
    }

    @Override
    protected void postAction(TransferProcess process) {
        observable.invokeForEach(l -> l.cancelled(process));
    }
}
