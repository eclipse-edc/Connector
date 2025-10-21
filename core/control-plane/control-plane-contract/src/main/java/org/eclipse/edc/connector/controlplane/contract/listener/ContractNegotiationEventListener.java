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

package org.eclipse.edc.connector.controlplane.contract.listener;

import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationAccepted;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationAgreed;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationEvent;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationInitiated;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationOffered;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationRequested;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationTerminated;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationVerified;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationListener;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.event.EventRouter;

public class ContractNegotiationEventListener implements ContractNegotiationListener {
    private final EventRouter eventRouter;

    public ContractNegotiationEventListener(EventRouter eventRouter) {
        this.eventRouter = eventRouter;
    }

    @Override
    public void initiated(ContractNegotiation negotiation) {
        var event = baseBuilder(ContractNegotiationInitiated.Builder.newInstance(), negotiation).build();
        eventRouter.publish(event);
    }

    @Override
    public void requested(ContractNegotiation negotiation) {
        var event = baseBuilder(ContractNegotiationRequested.Builder.newInstance(), negotiation).build();
        eventRouter.publish(event);
    }

    @Override
    public void offered(ContractNegotiation negotiation) {
        var event = baseBuilder(ContractNegotiationOffered.Builder.newInstance(), negotiation).build();
        eventRouter.publish(event);
    }

    @Override
    public void accepted(ContractNegotiation negotiation) {
        var event = baseBuilder(ContractNegotiationAccepted.Builder.newInstance(), negotiation).build();
        eventRouter.publish(event);
    }

    @Override
    public void terminated(ContractNegotiation negotiation) {
        var event = baseBuilder(ContractNegotiationTerminated.Builder.newInstance(), negotiation).build();
        eventRouter.publish(event);
    }

    @Override
    public void agreed(ContractNegotiation negotiation) {
        var event = baseBuilder(ContractNegotiationAgreed.Builder.newInstance(), negotiation).build();
        eventRouter.publish(event);
    }

    @Override
    public void verified(ContractNegotiation negotiation) {
        var event = baseBuilder(ContractNegotiationVerified.Builder.newInstance(), negotiation).build();
        eventRouter.publish(event);
    }

    @Override
    public void finalized(ContractNegotiation negotiation) {
        var event = baseBuilder(ContractNegotiationFinalized.Builder.newInstance(), negotiation)
                .contractAgreement(negotiation.getContractAgreement())
                .build();
        eventRouter.publish(event);
    }

    private <T extends ContractNegotiationEvent, B extends ContractNegotiationEvent.Builder<T, B>> B baseBuilder(B builder, ContractNegotiation negotiation) {
        return builder.contractNegotiationId(negotiation.getId())
                .counterPartyAddress(negotiation.getCounterPartyAddress())
                .callbackAddresses(negotiation.getCallbackAddresses())
                .contractOffers(negotiation.getContractOffers())
                .counterPartyId(negotiation.getCounterPartyId())
                .participantContextId(negotiation.getParticipantContextId())
                .protocol(negotiation.getProtocol());
    }

}
