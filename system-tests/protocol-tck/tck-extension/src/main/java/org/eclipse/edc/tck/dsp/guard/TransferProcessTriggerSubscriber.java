/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.tck.dsp.guard;

import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessEvent;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.persistence.StateEntityStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Fires triggers based on transfer events.
 */
public class TransferProcessTriggerSubscriber implements EventSubscriber, TransferProcessTriggerRegistry {
    private final List<Trigger<TransferProcess>> triggers = new ArrayList<>();
    private final StateEntityStore<TransferProcess> store;

    public TransferProcessTriggerSubscriber(StateEntityStore<TransferProcess> store) {
        this.store = store;
    }

    @Override
    public void register(Trigger<TransferProcess> trigger) {
        triggers.add(trigger);
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> envelope) {
        triggers.stream()
                .filter(trigger -> trigger.predicate().test(envelope.getPayload()))
                .forEach(trigger -> {
                    var event = (TransferProcessEvent) envelope.getPayload();
                    var negotiation = store.findByIdAndLease(event.getTransferProcessId()).getContent();
                    trigger.action().accept(negotiation);
                    store.save(negotiation);
                });
    }

}
