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
import org.eclipse.edc.connector.transfer.spi.types.command.NotifyStartedTransferCommand;

import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.CONSUMER;

/**
 * Puts a TransferProcess in the STARTED state as the counter-party actually started the transfer.
 */
public class NotifyStartedTransferCommandHandler extends SingleTransferProcessCommandHandler<NotifyStartedTransferCommand> {

    private final TransferProcessObservable observable;

    public NotifyStartedTransferCommandHandler(TransferProcessStore store, TransferProcessObservable observable) {
        super(store);
        this.observable = observable;
    }

    @Override
    public Class<NotifyStartedTransferCommand> getType() {
        return NotifyStartedTransferCommand.class;
    }

    @Override
    protected boolean modify(TransferProcess process, NotifyStartedTransferCommand command) {
        if (process.getType() == CONSUMER && process.canBeStartedConsumer()) {
            observable.invokeForEach(l -> l.preStarted(process));
            process.transitionStarted();
            return true;
        }

        return false;
    }

    @Override
    protected void postAction(TransferProcess process) {
        observable.invokeForEach(l -> l.started(process));
    }
}
