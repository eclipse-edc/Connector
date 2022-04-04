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

package org.eclipse.dataspaceconnector.transfer.core.command.handlers;

import org.eclipse.dataspaceconnector.spi.command.CommandHandler;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionResult;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.AddProvisionedResourceCommand;
import org.eclipse.dataspaceconnector.transfer.core.transfer.ProvisionCallbackDelegate;

import java.util.List;

/**
 * Processes a {@link AddProvisionedResourceCommand} by delegating it to the {@link TransferProcessManager}.
 * <p>
 * This class exists to avoid coupling the TPM to the command handler registry.
 */
public class AddProvisionedResourceCommandHandler implements CommandHandler<AddProvisionedResourceCommand> {
    private final ProvisionCallbackDelegate delegate;

    public AddProvisionedResourceCommandHandler(ProvisionCallbackDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handle(AddProvisionedResourceCommand command) {
        delegate.handleProvisionResult(command.getTransferProcessId(), List.of(ProvisionResult.success(command.getProvisionResponse())));
    }

    @Override
    public Class<AddProvisionedResourceCommand> getType() {
        return AddProvisionedResourceCommand.class;
    }

}
