/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.transfer.core.command.commands;

import org.eclipse.dataspaceconnector.spi.command.Command;
import org.eclipse.dataspaceconnector.transfer.core.command.handlers.TransferProcessCommandHandler;

/**
 * Specialization of the {@link Command} interface, that is useful in situations where
 * a single {@link org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess} is
 * operated on.
 *
 * @see TransferProcessCommandHandler
 */
public class TransferProcessCommand extends Command {
    protected final String transferProcessId;

    public TransferProcessCommand(String transferProcessId) {
        this.transferProcessId = transferProcessId;
    }

    public String getTransferProcessId() {
        return transferProcessId;
    }
}
