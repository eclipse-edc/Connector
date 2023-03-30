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
import org.eclipse.edc.connector.transfer.spi.types.command.NotifyTerminatedTransfer;

/**
 * Puts a TransferProcess in the TERMINATED state as the counter-party actually completed the transfer.
 */
public class NotifyTerminatedTransferCommandHandler extends SingleTransferProcessCommandHandler<NotifyTerminatedTransfer> {

    private final TransferProcessObservable observable;

    public NotifyTerminatedTransferCommandHandler(TransferProcessStore store, TransferProcessObservable observable) {
        super(store);
        this.observable = observable;
    }

    @Override
    public Class<NotifyTerminatedTransfer> getType() {
        return NotifyTerminatedTransfer.class;
    }

    @Override
    protected boolean modify(TransferProcess process, NotifyTerminatedTransfer command) {
        if (process.canBeTerminated()) {
            observable.invokeForEach(l -> l.preTerminated(process));
            process.transitionTerminated();
            return true;
        }

        return false;
    }

    @Override
    protected void postAction(TransferProcess process) {
        observable.invokeForEach(l -> l.terminated(process));
    }
}
