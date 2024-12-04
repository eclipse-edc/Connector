/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationEvent;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.system.ServiceExtension;

import static org.eclipse.edc.tck.dsp.data.DataAssembly.createNegotiationRecorder;
import static org.eclipse.edc.tck.dsp.data.DataAssembly.createNegotiationTriggers;

/**
 * Loads the transition guard.
 */
public class TckGuardExtension implements ServiceExtension {
    private static final String NAME = "DSP TCK Guard";

    private ContractNegotiationGuard negotiationGuard;

    @Inject
    private ContractNegotiationStore store;

    @Inject
    private EventRouter router;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public ContractNegotiationPendingGuard negotiationGuard() {
        var recorder = createNegotiationRecorder();

        var registry = new ContractNegotiationTriggerSubscriber(store);
        createNegotiationTriggers().forEach(registry::register);
        router.register(ContractNegotiationEvent.class, registry);

        negotiationGuard = new ContractNegotiationGuard(cn -> recorder.playNext(cn.getContractOffers().get(0).getAssetId(), cn), store);
        return negotiationGuard;
    }

    @Override
    public void prepare() {
        if (negotiationGuard != null) {
            negotiationGuard.start();
        }
    }

    @Override
    public void shutdown() {
        if (negotiationGuard != null) {
            negotiationGuard.stop();
        }
    }
}
