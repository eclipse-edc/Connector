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

package org.eclipse.dataspaceconnector.contract.listener;

import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationListener;
import org.eclipse.dataspaceconnector.spi.event.EventRouter;
import org.eclipse.dataspaceconnector.spi.event.contractnegotiation.ContractNegotiationApproved;
import org.eclipse.dataspaceconnector.spi.event.contractnegotiation.ContractNegotiationConfirmed;
import org.eclipse.dataspaceconnector.spi.event.contractnegotiation.ContractNegotiationDeclined;
import org.eclipse.dataspaceconnector.spi.event.contractnegotiation.ContractNegotiationFailed;
import org.eclipse.dataspaceconnector.spi.event.contractnegotiation.ContractNegotiationInitiated;
import org.eclipse.dataspaceconnector.spi.event.contractnegotiation.ContractNegotiationOffered;
import org.eclipse.dataspaceconnector.spi.event.contractnegotiation.ContractNegotiationRequested;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;

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
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void requested(ContractNegotiation negotiation) {
        var event = ContractNegotiationRequested.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void offered(ContractNegotiation negotiation) {
        var event = ContractNegotiationOffered.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void approved(ContractNegotiation negotiation) {
        var event = ContractNegotiationApproved.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void declined(ContractNegotiation negotiation) {
        var event = ContractNegotiationDeclined.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void confirmed(ContractNegotiation negotiation) {
        var event = ContractNegotiationConfirmed.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }

    @Override
    public void failed(ContractNegotiation negotiation) {
        var event = ContractNegotiationFailed.Builder.newInstance()
                .contractNegotiationId(negotiation.getId())
                .at(clock.millis())
                .build();

        eventRouter.publish(event);
    }
}
