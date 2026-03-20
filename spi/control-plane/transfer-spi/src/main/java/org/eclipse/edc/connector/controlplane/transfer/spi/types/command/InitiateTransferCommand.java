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

import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.command.EntityCommand;

import java.util.UUID;

/**
 * Initiates a TransferProcess on the Consumer side
 */
public class InitiateTransferCommand extends EntityCommand {

    private final ParticipantContext participantContext;
    private final TransferRequest request;

    public InitiateTransferCommand(ParticipantContext participantContext, TransferRequest request) {
        super(UUID.randomUUID().toString());
        this.participantContext = participantContext;
        this.request = request;
    }

    public ParticipantContext getParticipantContext() {
        return participantContext;
    }

    public TransferRequest getRequest() {
        return request;
    }
}
