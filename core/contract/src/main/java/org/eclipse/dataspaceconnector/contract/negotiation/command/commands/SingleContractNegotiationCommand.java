/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.contract.negotiation.command.commands;

import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.command.ContractNegotiationCommand;

/**
 * Sub-type of {@link ContractNegotiationCommand} that references a specific ContractNegotiation
 * by ID.
 */
public class SingleContractNegotiationCommand extends ContractNegotiationCommand {
    
    protected final String negotiationId;
    
    public SingleContractNegotiationCommand(String negotiationId) {
        super();
        this.negotiationId = negotiationId;
    }
    
    public String getNegotiationId() {
        return negotiationId;
    }
    
}
