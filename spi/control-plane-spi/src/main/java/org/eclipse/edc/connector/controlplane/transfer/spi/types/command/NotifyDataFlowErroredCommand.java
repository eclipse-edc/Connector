/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.spi.types.command;

import org.eclipse.edc.spi.command.EntityCommand;

/**
 * Informs the transfer process that the data plane reported a non-recoverable error of its data flow. The transfer
 * process doesn't change state.
 */
public class NotifyDataFlowErroredCommand extends EntityCommand {

    private final String error;

    public NotifyDataFlowErroredCommand(String transferProcessId, String error) {
        super(transferProcessId);
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
