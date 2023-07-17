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

import org.eclipse.edc.connector.transfer.provision.DeprovisionResponsesHandler;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.command.DeprovisionCompleteCommand;
import org.eclipse.edc.spi.command.SingleEntityCommandHandler;
import org.eclipse.edc.spi.response.StatusResult;

import java.util.List;

/**
 * Handles a {@link DeprovisionCompleteCommand}.
 */
public class DeprovisionCompleteCommandHandler extends SingleEntityCommandHandler<DeprovisionCompleteCommand, TransferProcess> {

    private final DeprovisionResponsesHandler deprovisionResponsesHandler;

    public DeprovisionCompleteCommandHandler(TransferProcessStore store, DeprovisionResponsesHandler deprovisionResponsesHandler) {
        super(store);
        this.deprovisionResponsesHandler = deprovisionResponsesHandler;
    }

    @Override
    public Class<DeprovisionCompleteCommand> getType() {
        return DeprovisionCompleteCommand.class;
    }

    @Override
    protected boolean modify(TransferProcess entity, DeprovisionCompleteCommand command) {
        return deprovisionResponsesHandler.handle(entity, List.of(StatusResult.success(command.getResource())));
    }

    @Override
    public void postActions(TransferProcess entity, DeprovisionCompleteCommand command) {
        deprovisionResponsesHandler.postActions(entity);
    }
}
