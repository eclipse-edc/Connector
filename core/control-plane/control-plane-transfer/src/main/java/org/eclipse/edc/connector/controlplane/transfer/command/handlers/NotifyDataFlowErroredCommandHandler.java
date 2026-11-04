/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.command.handlers;

import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.NotifyDataFlowErroredCommand;
import org.eclipse.edc.spi.command.CommandHandler;
import org.eclipse.edc.spi.command.CommandResult;

/**
 * Handles a {@link NotifyDataFlowErroredCommand}. As defined by the Data Plane Signaling specification, the transfer
 * process doesn't change state and the counter-party is not notified: the listeners are notified, so that the error
 * can be acted upon, e.g. through the emitted event.
 */
public class NotifyDataFlowErroredCommandHandler implements CommandHandler<NotifyDataFlowErroredCommand> {

    private final TransferProcessStore store;
    private final TransferProcessObservable observable;

    public NotifyDataFlowErroredCommandHandler(TransferProcessStore store, TransferProcessObservable observable) {
        this.store = store;
        this.observable = observable;
    }

    @Override
    public Class<NotifyDataFlowErroredCommand> getType() {
        return NotifyDataFlowErroredCommand.class;
    }

    @Override
    public CommandResult handle(NotifyDataFlowErroredCommand command) {
        var transferProcess = store.findById(command.getEntityId());
        if (transferProcess == null) {
            return CommandResult.notFound("TransferProcess %s does not exist".formatted(command.getEntityId()));
        }

        observable.invokeForEach(l -> l.dataFlowErrored(transferProcess, command.getError()));
        return CommandResult.success();
    }
}
