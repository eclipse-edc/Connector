/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.tck.dsp.guard;

import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationEvent;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.persistence.StateEntityStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Fires triggers based on negotiation events.
 */
public class ContractNegotiationTriggerSubscriber implements EventSubscriber, ContractNegotiationTriggerRegistry {
    private final StateEntityStore<ContractNegotiation> store;

    private final List<Trigger<ContractNegotiation>> triggers = new ArrayList<>();

    public ContractNegotiationTriggerSubscriber(StateEntityStore<ContractNegotiation> store) {
        this.store = store;
    }

    @Override
    public void register(Trigger<ContractNegotiation> trigger) {
        triggers.add(trigger);
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> envelope) {
        triggers.stream()
                .filter(trigger -> trigger.predicate().test(envelope.getPayload()))
                .forEach(trigger -> {
                    var event = (ContractNegotiationEvent) envelope.getPayload();
                    var negotiation = store.findByIdAndLease(event.getContractNegotiationId()).getContent();
                    trigger.action().accept(negotiation);
                    store.save(negotiation);
                });
    }

}
