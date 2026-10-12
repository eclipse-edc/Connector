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
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.ApproveNegotiationCommand;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.command.EntityCommandHandler;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;

/**
 * Handler for {@link ApproveNegotiationCommand}s. Approves a provider ContractNegotiation held in the REQUESTED state,
 * transitioning it to AGREEING.
 */
public class ApproveNegotiationCommandHandler extends EntityCommandHandler<ApproveNegotiationCommand, ContractNegotiation> {

    private final ContractNegotiationObservable observable;

    public ApproveNegotiationCommandHandler(ContractNegotiationStore store, ContractNegotiationObservable observable) {
        super(store);
        this.observable = observable;
    }

    @Override
    public Class<ApproveNegotiationCommand> getType() {
        return ApproveNegotiationCommand.class;
    }

    @Override
    protected boolean modify(ContractNegotiation negotiation, ApproveNegotiationCommand command) {
        if (negotiation.getType() != PROVIDER || !negotiation.currentStateIsOneOf(REQUESTED) || !negotiation.isPending()) {
            return false;
        }
        negotiation.transitionAgreeing();
        negotiation.setPending(false);
        return true;
    }

    @Override
    public void postActions(ContractNegotiation negotiation, ApproveNegotiationCommand command) {
        observable.invokeForEach(l -> l.approved(negotiation));
    }
}
