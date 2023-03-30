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

import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.command.NotifyCompletedTransfer;

/**
 * Puts a TransferProcess in the COMPLETED state as the counter-party actually completed the transfer.
 */
public class NotifyCompletedTransferCommandHandler extends SingleTransferProcessCommandHandler<NotifyCompletedTransfer> {

    private final TransferProcessObservable observable;

    public NotifyCompletedTransferCommandHandler(TransferProcessStore store, TransferProcessObservable observable) {
        super(store);
        this.observable = observable;
    }

    @Override
    public Class<NotifyCompletedTransfer> getType() {
        return NotifyCompletedTransfer.class;
    }

    @Override
    protected boolean modify(TransferProcess process, NotifyCompletedTransfer command) {
        if (process.canBeCompleted()) {
            observable.invokeForEach(l -> l.preCompleted(process));
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
