/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.command.handlers;

import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessStartedData;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.NotifyStartedCommand;
import org.eclipse.edc.spi.command.EntityCommandHandler;

/**
 * Handles a {@link NotifyStartedCommand}.
 */
public class NotifyStartedCommandHandler extends EntityCommandHandler<NotifyStartedCommand, TransferProcess> {

    private final TransferProcessObservable observable;
    private final DataAddressStore dataAddressStore;

    public NotifyStartedCommandHandler(TransferProcessStore store, TransferProcessObservable observable, DataAddressStore dataAddressStore) {
        super(store);
        this.observable = observable;
        this.dataAddressStore = dataAddressStore;
    }

    @Override
    public Class<NotifyStartedCommand> getType() {
        return NotifyStartedCommand.class;
    }

    @Override
    protected boolean modify(TransferProcess entity, NotifyStartedCommand command) {
        if (entity.getType() == TransferProcess.Type.CONSUMER) {
            return false;
        }

        entity.transitionStarting();

        if (command.getDataAddress() != null) {
            var dataAddressStorage = dataAddressStore.store(command.getDataAddress(), entity);
            return !dataAddressStorage.failed();
        }

        return true;
    }

    @Override
    public void postActions(TransferProcess entity, NotifyStartedCommand command) {
        observable.invokeForEach(l -> l.started(entity, TransferProcessStartedData.Builder.newInstance().dataAddress(entity.getContentDataAddress()).build()));
    }
}
