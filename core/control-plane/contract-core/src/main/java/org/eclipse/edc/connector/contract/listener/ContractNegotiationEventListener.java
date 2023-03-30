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

package org.eclipse.edc.connector.contract.listener;

import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationListener;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.contractnegotiation.ContractNegotiationConsumerAgreed;
import org.eclipse.edc.spi.event.contractnegotiation.ContractNegotiationConsumerRequested;
import org.eclipse.edc.spi.event.contractnegotiation.ContractNegotiationConsumerVerified;
import org.eclipse.edc.spi.event.contractnegotiation.ContractNegotiationDeclined;
import org.eclipse.edc.spi.event.contractnegotiation.ContractNegotiationEvent;
import org.eclipse.edc.spi.event.contractnegotiation.ContractNegotiationFailed;
import org.eclipse.edc.spi.event.contractnegotiation.ContractNegotiationInitiated;
import org.eclipse.edc.spi.event.contractnegotiation.ContractNegotiationProviderAgreed;
import org.eclipse.edc.spi.event.contractnegotiation.ContractNegotiationProviderFinalized;
import org.eclipse.edc.spi.event.contractnegotiation.ContractNegotiationProviderOffered;
import org.eclipse.edc.spi.event.contractnegotiation.ContractNegotiationTerminated;

import java.time.Clock;

public class ContractNegotiationEventListener implements ContractNegotiationListener {
    private final EventRouter eventRouter;
    private final Clock clock;

    public ContractNegotiationEventListener(EventRouter eventRouter, Clock clock) {
        this.eventRouter = eventRouter;
        this.clock = clock;
    }

    @Override
    public void initiated(ContractNegotiation negotiation) {
        var event = ContractNegotiationInitiated.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .build();

        publish(event);
    }

    @Override
    public void consumerRequested(ContractNegotiation negotiation) {
        var event = ContractNegotiationConsumerRequested.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .build();

        publish(event);
    }

    @Override
    public void providerOffered(ContractNegotiation negotiation) {
        var event = ContractNegotiationProviderOffered.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .build();

        publish(event);
    }

    @Override
    public void consumerAgreed(ContractNegotiation negotiation) {
        var event = ContractNegotiationConsumerAgreed.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .build();

        publish(event);
    }

    @Override
    public void terminated(ContractNegotiation negotiation) {
        var event = ContractNegotiationTerminated.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .build();

        publish(event);
    }

    @Override
    public void declined(ContractNegotiation negotiation) {
        var event = ContractNegotiationDeclined.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .build();

        publish(event);
    }

    @Override
    public void providerAgreed(ContractNegotiation negotiation) {
        var event = ContractNegotiationProviderAgreed.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .build();

        publish(event);
    }

    @Override
    public void consumerVerified(ContractNegotiation negotiation) {
        var event = ContractNegotiationConsumerVerified.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .build();

        publish(event);
    }

    @Override
    public void providerFinalized(ContractNegotiation negotiation) {
        var event = ContractNegotiationProviderFinalized.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .build();

        publish(event);
    }

    @Override
    public void failed(ContractNegotiation negotiation) {
        var event = ContractNegotiationFailed.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .build();

        publish(event);
    }

    private void publish(ContractNegotiationEvent event) {
        var envelope = EventEnvelope.Builder.newInstance()
                .payload(event)
                .at(clock.millis())
                .build();
        eventRouter.publish(envelope);
    }
}
