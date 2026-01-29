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
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.command.handlers;

import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessStartedData;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.NotifyPreparedCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.NotifyStartedCommand;
import org.eclipse.edc.spi.command.EntityCommandHandler;

/**
 * Handles a {@link NotifyPreparedCommand}.
 */
public class NotifyStartedCommandHandler extends EntityCommandHandler<NotifyStartedCommand, TransferProcess> {

    // TODO: unit tests
    private final TransferProcessObservable observable;

    public NotifyStartedCommandHandler(TransferProcessStore store, TransferProcessObservable observable) {
        super(store);
        this.observable = observable;
    }

    @Override
    public Class<NotifyStartedCommand> getType() {
        return NotifyStartedCommand.class;
    }

    @Override
    protected boolean modify(TransferProcess entity, NotifyStartedCommand command) {
        // TODO: unit tests
        if (command.getDataAddress() != null) {
            entity.updateDestination(command.getDataAddress());
        }

        if (entity.getType() == TransferProcess.Type.CONSUMER) {
            return false; // TODO: what?
        } else {
            entity.transitionStarting();
        }
        return true;
    }

    @Override
    public void postActions(TransferProcess entity, NotifyStartedCommand command) {
        observable.invokeForEach(l -> l.started(entity, TransferProcessStartedData.Builder.newInstance().dataAddress(entity.getContentDataAddress()).build()));
    }
}
