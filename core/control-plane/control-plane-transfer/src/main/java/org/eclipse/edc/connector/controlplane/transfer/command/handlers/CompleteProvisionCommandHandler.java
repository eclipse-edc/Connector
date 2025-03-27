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
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.CompleteProvisionCommand;
import org.eclipse.edc.spi.command.EntityCommandHandler;

/**
 * Handles a {@link CompleteProvisionCommand}.
 */
public class CompleteProvisionCommandHandler extends EntityCommandHandler<CompleteProvisionCommand, TransferProcess> {

    private final TransferProcessObservable observable;

    public CompleteProvisionCommandHandler(TransferProcessStore store, TransferProcessObservable observable) {
        super(store);
        this.observable = observable;
    }

    @Override
    public Class<CompleteProvisionCommand> getType() {
        return CompleteProvisionCommand.class;
    }

    @Override
    protected boolean modify(TransferProcess entity, CompleteProvisionCommand command) {
        if (command.getNewAddress() != null) {
            entity.updateDestination(command.getNewAddress());
        }
        entity.transitionProvisioned();
        return true;
    }

    @Override
    public void postActions(TransferProcess entity, CompleteProvisionCommand command) {
        observable.invokeForEach(l -> l.provisioned(entity));
    }
}
