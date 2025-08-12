/*
 *  Copyright (c) 2021 - 2022 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.contract.negotiation.command.handlers;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.command.EntityCommandHandler;

/**
 * Handler for {@link TerminateNegotiationCommand}s. Transitions the specified ContractNegotiation to the TERMINATING state.
 */
public class TerminateNegotiationCommandHandler extends EntityCommandHandler<TerminateNegotiationCommand, ContractNegotiation> {

    public TerminateNegotiationCommandHandler(ContractNegotiationStore store) {
        super(store);
    }

    @Override
    public Class<TerminateNegotiationCommand> getType() {
        return TerminateNegotiationCommand.class;
    }

    /**
     * Transitions a {@link ContractNegotiation} to the error state.
     *
     * @param negotiation the ContractNegotiation to modify.
     * @return true
     */
    @Override
    protected boolean modify(ContractNegotiation negotiation, TerminateNegotiationCommand command) {
        negotiation.transitionTerminating(command.getReason());
        return true;
    }

}
