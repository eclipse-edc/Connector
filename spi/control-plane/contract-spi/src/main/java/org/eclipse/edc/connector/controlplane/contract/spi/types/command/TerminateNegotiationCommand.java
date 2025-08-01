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

package org.eclipse.edc.connector.controlplane.contract.spi.types.command;

import org.eclipse.edc.spi.command.EntityCommand;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class TerminateNegotiationCommand extends EntityCommand {

    public static final String TERMINATE_NEGOTIATION_TYPE_TERM = "TerminateNegotiation";
    public static final String TERMINATE_NEGOTIATION_TYPE = EDC_NAMESPACE + TERMINATE_NEGOTIATION_TYPE_TERM;
    public static final String TERMINATE_NEGOTIATION_REASON = EDC_NAMESPACE + "reason";

    private final String reason;

    public TerminateNegotiationCommand(String negotiationId, String reason) {
        super(negotiationId);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
