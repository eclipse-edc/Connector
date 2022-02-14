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
 *       Fraunhofer Institute for Software and Systems Engineering - refactored
 *
 */
package org.eclipse.dataspaceconnector.transfer.core.command.commands;

import org.eclipse.dataspaceconnector.spi.command.Command;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.TransferProcessCommand;
import org.eclipse.dataspaceconnector.transfer.core.command.handlers.SingleTransferProcessCommandHandler;

/**
 * Specialization of the {@link Command} interface, that is useful in situations where
 * a single {@link org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess} is
 * operated on.
 *
 * @see SingleTransferProcessCommandHandler
 */
public class SingleTransferProcessCommand extends TransferProcessCommand {
    protected final String transferProcessId;

    public SingleTransferProcessCommand(String transferProcessId) {
        super();
        this.transferProcessId = transferProcessId;
    }

    public String getTransferProcessId() {
        return transferProcessId;
    }
}
