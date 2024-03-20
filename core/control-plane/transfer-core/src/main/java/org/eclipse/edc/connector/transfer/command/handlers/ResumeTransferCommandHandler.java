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

package org.eclipse.edc.connector.transfer.command.handlers;

import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.transfer.spi.types.command.ResumeTransferCommand;
import org.eclipse.edc.spi.command.EntityCommandHandler;

import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.SUSPENDED;

/**
 * Resumes a SUSPENDED transfer process and puts it in the {@link TransferProcessStates#STARTING} state.
 */
public class ResumeTransferCommandHandler extends EntityCommandHandler<ResumeTransferCommand, TransferProcess> {

    public ResumeTransferCommandHandler(TransferProcessStore store) {
        super(store);
    }

    @Override
    public Class<ResumeTransferCommand> getType() {
        return ResumeTransferCommand.class;
    }

    @Override
    protected boolean modify(TransferProcess process, ResumeTransferCommand command) {
        if (process.currentStateIsOneOf(SUSPENDED)) {
            process.transitionStarting();
            return true;
        }

        return false;
    }

}
