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

package org.eclipse.edc.connector.contract.negotiation.command.handlers;

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.command.SingleContractNegotiationCommand;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.command.CommandHandler;

import static java.lang.String.format;

/**
 * Abstract handler for {@link SingleContractNegotiationCommand}s. Fetches the {@link ContractNegotiation}
 * from the {@link ContractNegotiationStore} before calling its sub-classes' custom logic.
 *
 * @param <T> the sub type of SingleContractNegotiationCommand this handler can handle.
 */
public abstract class SingleContractNegotiationCommandHandler<T extends SingleContractNegotiationCommand> implements CommandHandler<T> {

    protected final ContractNegotiationStore store;

    public SingleContractNegotiationCommandHandler(ContractNegotiationStore store) {
        this.store = store;
    }

    /**
     * Fetches the {@link ContractNegotiation} specified by the ID in the command from the store.
     * Then calls the {@link #modify(ContractNegotiation)} method defined in the respective
     * sub-class this method was called on.
     *
     * @param command the command.
     */
    @Override
    public void handle(SingleContractNegotiationCommand command) {
        var negotiationId = command.getNegotiationId();
        var negotiation = store.findById(negotiationId);
        if (negotiation == null) {
            throw new EdcException(format("Could not find ContractNegotiation with ID [%s]", negotiationId));
        } else {
            if (modify(negotiation)) {
                negotiation.setModified();
                store.save(negotiation);
            }
        }
    }

    /**
     * Should contain the logic to modify the ContractNegotiation. To be implemented by sub-classes.
     * Will not be called if the ContractNegotiation specified by the command could not be fetched
     * from the store. Should return {@code true}, if the ContractNegotiation was modified;
     * {@code false} otherwise.
     *
     * @param negotiation the ContractNegotiation to modify.
     * @return true, if the ContractNegotiation was modified; false otherwise.
     */
    protected abstract boolean modify(ContractNegotiation negotiation);

}
