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

package org.eclipse.edc.connector.controlplane.transfer.spi.types.command;

import org.eclipse.edc.spi.command.EntityCommand;
import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * Informs the transfer process that the startup phase has been completed
 */
public class NotifyStartedCommand extends EntityCommand {

    private final DataAddress dataAddress;

    public NotifyStartedCommand(String transferProcessId, DataAddress dataAddress) {
        super(transferProcessId);
        this.dataAddress = dataAddress;
    }

    public DataAddress getDataAddress() {
        return dataAddress;
    }
}
