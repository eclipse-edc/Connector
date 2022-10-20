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
 *       Fraunhofer Institute for Software and Systems Engineering - refactored
 *
 */

package org.eclipse.dataspaceconnector.spi.types.domain.transfer.command;

/**
 * Fail a transfer process by sending it to the ERROR state
 */
public class FailTransferCommand extends SingleTransferProcessCommand {

    private String errorMessage;

    public FailTransferCommand(String transferProcessId, String errorMessage) {
        super(transferProcessId);
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
