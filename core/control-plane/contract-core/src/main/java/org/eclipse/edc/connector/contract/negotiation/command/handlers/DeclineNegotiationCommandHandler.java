/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.contract.negotiation.command.handlers;

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.command.DeclineNegotiationCommand;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;

/**
 * Handler for {@link DeclineNegotiationCommand}s. Transitions the specified ContractNegotiation to the DECLINING state.
 */
public class DeclineNegotiationCommandHandler extends SingleContractNegotiationCommandHandler<DeclineNegotiationCommand> {

    public DeclineNegotiationCommandHandler(ContractNegotiationStore store) {
        super(store);
    }

    @Override
    public Class<DeclineNegotiationCommand> getType() {
        return DeclineNegotiationCommand.class;
    }

    /**
     * Transitions a {@link ContractNegotiation} to the DECLINING state.
     *
     * @param negotiation the ContractNegotiation to modify.
     * @return true
     */
    @Override
    protected boolean modify(ContractNegotiation negotiation) {
        negotiation.transitionDeclining();
        return true;
    }

}
