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
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.NotifyPreparedCommand;
import org.eclipse.edc.spi.command.EntityCommandHandler;

/**
 * Handles a {@link NotifyPreparedCommand}.
 */
public class NotifyPreparedCommandHandler extends EntityCommandHandler<NotifyPreparedCommand, TransferProcess> {

    private final TransferProcessObservable observable;
    private final DataAddressStore dataAddressStore;

    public NotifyPreparedCommandHandler(TransferProcessStore store, TransferProcessObservable observable, DataAddressStore dataAddressStore) {
        super(store);
        this.observable = observable;
        this.dataAddressStore = dataAddressStore;
    }

    @Override
    public Class<NotifyPreparedCommand> getType() {
        return NotifyPreparedCommand.class;
    }

    @Override
    protected boolean modify(TransferProcess entity, NotifyPreparedCommand command) {
        if (entity.getType() == TransferProcess.Type.CONSUMER) {
            entity.transitionRequesting();
        } else {
            entity.transitionStarting();
        }

        if (command.getDataAddress() != null) {
            var dataAddressStorage = dataAddressStore.store(command.getDataAddress(), entity);
            return !dataAddressStorage.failed();
        }

        return true;
    }

    @Override
    public void postActions(TransferProcess entity, NotifyPreparedCommand command) {
        observable.invokeForEach(l -> l.provisioned(entity));
    }
}
