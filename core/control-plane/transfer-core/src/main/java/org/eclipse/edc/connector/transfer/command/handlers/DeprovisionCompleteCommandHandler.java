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

package org.eclipse.edc.connector.transfer.command.handlers;

import org.eclipse.edc.connector.transfer.process.ProvisionCallbackDelegate;
import org.eclipse.edc.connector.transfer.spi.types.command.DeprovisionCompleteCommand;
import org.eclipse.edc.spi.command.CommandHandler;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;

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
    public Result<Void> handle(DeprovisionCompleteCommand command) {
        return delegate.handleDeprovisionResult(command.getTransferProcessId(), List.of(StatusResult.success(command.getResource())));
    }

    @Override
    public Class<DeprovisionCompleteCommand> getType() {
        return DeprovisionCompleteCommand.class;
    }
}
