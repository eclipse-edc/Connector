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

package org.eclipse.dataspaceconnector.contract.negotiation.command.handlers;

import org.eclipse.dataspaceconnector.contract.negotiation.command.commands.CancelNegotiationCommand;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;

/**
 * Handler for {@link CancelNegotiationCommand}s. Transitions the specified ContractNegotiation
 * to the error state.
 */
public class CancelNegotiationCommandHandler extends SingleContractNegotiationCommandHandler<CancelNegotiationCommand> {
    
    public CancelNegotiationCommandHandler(ContractNegotiationStore store) {
        super(store);
    }
    
    @Override
    public Class<CancelNegotiationCommand> getType() {
        return CancelNegotiationCommand.class;
    }
    
    /**
     * Transitions a {@link ContractNegotiation} to the error state.
     *
     * @param negotiation the ContractNegotiation to modify.
     * @return true
     */
    @Override
    protected boolean modify(ContractNegotiation negotiation) {
        negotiation.transitionError("Cancelled");
        return true;
    }
    
}
