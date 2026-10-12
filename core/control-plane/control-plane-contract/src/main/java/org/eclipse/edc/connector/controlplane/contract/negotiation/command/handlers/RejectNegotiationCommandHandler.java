/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.contract.negotiation.command.handlers;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.RejectNegotiationCommand;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.command.EntityCommandHandler;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;

/**
 * Handler for {@link RejectNegotiationCommand}s. Rejects a provider ContractNegotiation held in the REQUESTED state,
 * transitioning it to TERMINATING.
 * <p>
 * The termination reason is sent to the counter-party, so a fixed reason is used to avoid disclosing any provider-internal
 * information.
 */
public class RejectNegotiationCommandHandler extends EntityCommandHandler<RejectNegotiationCommand, ContractNegotiation> {

    public static final String REJECTION_REASON = "Negotiation manually rejected";

    private final ContractNegotiationObservable observable;

    public RejectNegotiationCommandHandler(ContractNegotiationStore store, ContractNegotiationObservable observable) {
        super(store);
        this.observable = observable;
    }

    @Override
    public Class<RejectNegotiationCommand> getType() {
        return RejectNegotiationCommand.class;
    }

    @Override
    protected boolean modify(ContractNegotiation negotiation, RejectNegotiationCommand command) {
        if (negotiation.getType() != PROVIDER || !negotiation.currentStateIsOneOf(REQUESTED) || !negotiation.isPending()) {
            return false;
        }
        negotiation.transitionTerminating(REJECTION_REASON);
        negotiation.setPending(false);
        return true;
    }

    @Override
    public void postActions(ContractNegotiation negotiation, RejectNegotiationCommand command) {
        observable.invokeForEach(l -> l.rejected(negotiation));
        observable.invokeForEach(l -> l.terminating(negotiation));
    }
}
