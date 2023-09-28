/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.spi.types.command;

import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.command.EntityCommand;

/**
 * Stops a transfer process by sending it to the STOPPING state.
 * The field subsequentState point to which state the TransferProcess should be put after flow stop.
 * It could be COMPLETING, TERMINATING or SUSPENDING.
 */
public class StopTransferCommand extends EntityCommand {

    private final String reason;
    private final TransferProcessStates subsequentState;

    public StopTransferCommand(String transferProcessId, String reason, TransferProcessStates subsequentState) {
        super(transferProcessId);
        this.reason = reason;
        this.subsequentState = subsequentState;
    }

    public String getReason() {
        return reason;
    }

    public TransferProcessStates getSubsequentState() {
        return subsequentState;
    }
}
