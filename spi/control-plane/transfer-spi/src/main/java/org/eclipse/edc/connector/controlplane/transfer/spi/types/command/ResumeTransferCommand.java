/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import org.eclipse.edc.spi.command.EntityCommand;

/**
 * Resumes a transfer process by sending it to the STARTED state
 */
public class ResumeTransferCommand extends EntityCommand {

    public ResumeTransferCommand(String transferProcessId) {
        super(transferProcessId);
    }

}
