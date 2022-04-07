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

package org.eclipse.dataspaceconnector.transfer.core.command.handlers;

import org.eclipse.dataspaceconnector.spi.command.CommandHandler;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.transfer.core.transfer.ProvisionCallbackDelegate;

import java.util.List;

/**
 * Handles a {@link DeprovisionCompleteCommand} and delegates it back to the {@link ProvisionCallbackDelegate}
 */
public class DeprovisionCompleteCommandHandler implements CommandHandler<DeprovisionCompleteCommand> {

    private final ProvisionCallbackDelegate delegate;

    public DeprovisionCompleteCommandHandler(ProvisionCallbackDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handle(DeprovisionCompleteCommand command) {
        delegate.handleDeprovisionResult(command.getTransferProcessId(), List.of(StatusResult.success(command.getResource())));
    }

    @Override
    public Class<DeprovisionCompleteCommand> getType() {
        return DeprovisionCompleteCommand.class;
    }
}
