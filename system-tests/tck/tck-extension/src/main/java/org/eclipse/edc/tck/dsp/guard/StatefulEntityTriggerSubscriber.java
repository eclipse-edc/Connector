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

import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.StateEntityStore;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Fires triggers based on negotiation events.
 */
public class StatefulEntityTriggerSubscriber<SE extends StatefulEntity<SE>, EP> implements EventSubscriber, StatefulEntityTriggerRegistry<SE> {
    private final List<Trigger<SE>> triggers = new ArrayList<>();
    private final Monitor monitor;
    private final StateEntityStore<SE> store;
    private final TransactionContext transactionContext;
    private final Class<EP> baseEventPayload;
    private final Function<EP, String> idFromEventPayload;
    private final int delayMillis;

    public StatefulEntityTriggerSubscriber(Monitor monitor, StateEntityStore<SE> store, TransactionContext transactionContext,
                                           Class<EP> eventPayloadBaseClass, Function<EP, String> idFromEventPayload) {
        this(monitor, store, transactionContext, eventPayloadBaseClass, idFromEventPayload, 50);
    }

    public StatefulEntityTriggerSubscriber(Monitor monitor, StateEntityStore<SE> store, TransactionContext transactionContext,
                                           Class<EP> eventPayloadBaseClass, Function<EP, String> idFromEventPayload, int delayMillis) {
        this.monitor = monitor;
        this.store = store;
        this.transactionContext = transactionContext;
        this.baseEventPayload = eventPayloadBaseClass;
        this.idFromEventPayload = idFromEventPayload;
        this.delayMillis = delayMillis;
    }

    @Override
    public void register(Trigger<SE> trigger) {
        triggers.add(trigger);
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> envelope) {

        try {
            TimeUnit.MILLISECONDS.sleep(delayMillis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        triggers.stream()
                .filter(trigger -> trigger.predicate().test(envelope.getPayload()))
                .forEach(trigger -> {
                    try {
                        var event = baseEventPayload.cast(envelope.getPayload());
                        transactionContext.execute(() -> {
                            var id = idFromEventPayload.apply(event);
                            store.findByIdAndLease(id)
                                    .onSuccess(entity -> {
                                        trigger.action().accept(entity);
                                        update(entity);
                                    }).onFailure(f -> {
                                        monitor.severe("Cannot find entity %s: %s, so trigger doesn't get executed. Event: %s"
                                                .formatted(id, f.getReason(), event.getClass().getSimpleName()));
                                    });

                        });
                    } catch (Exception e) {
                        monitor.severe("Generic error while trying to execute TCK trigger on event " + envelope.getPayload().name(), e);
                    }

                });
    }

    protected void update(SE entity) {
        store.save(entity);
        var error = entity.getErrorDetail() == null ? "" : ". errorDetail: " + entity.getErrorDetail();

        monitor.debug(() -> "[%s] %s %s is now in state %s%s"
                .formatted(this.getClass().getSimpleName(), entity.getClass().getSimpleName(),
                        entity.getId(), entity.stateAsString(), error));
    }

}
