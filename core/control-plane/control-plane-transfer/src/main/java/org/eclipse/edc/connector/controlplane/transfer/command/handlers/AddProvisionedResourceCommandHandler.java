/*
 *  Copyright (c) 2022 Microsoft Corporation
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

import org.eclipse.edc.connector.controlplane.transfer.provision.ProvisionResponsesHandler;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.AddProvisionedResourceCommand;
import org.eclipse.edc.spi.command.EntityCommandHandler;
import org.eclipse.edc.spi.response.StatusResult;

import java.util.List;

/**
 * Processes a {@link AddProvisionedResourceCommand}
 *
 * @deprecated provisioning will be fully managed by the data-plane
 */
@Deprecated(since = "0.14.1")
public class AddProvisionedResourceCommandHandler extends EntityCommandHandler<AddProvisionedResourceCommand, TransferProcess> {

    private final ProvisionResponsesHandler provisionResponsesHandler;

    public AddProvisionedResourceCommandHandler(TransferProcessStore store, ProvisionResponsesHandler provisionResponsesHandler) {
        super(store);
        this.provisionResponsesHandler = provisionResponsesHandler;
    }

    @Override
    public Class<AddProvisionedResourceCommand> getType() {
        return AddProvisionedResourceCommand.class;
    }

    @Override
    protected boolean modify(TransferProcess entity, AddProvisionedResourceCommand command) {
        return provisionResponsesHandler.handle(entity, List.of(StatusResult.success(command.getProvisionResponse())));
    }

    @Override
    public void postActions(TransferProcess entity, AddProvisionedResourceCommand command) {
        provisionResponsesHandler.postActions(entity);
    }
}
