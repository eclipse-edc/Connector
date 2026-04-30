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

package org.eclipse.edc.tasks.tck.dsp.guard;

import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationEvent;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessPendingGuard;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessEvent;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.controlplane.tasks.TaskService;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;

import static org.eclipse.edc.tasks.tck.dsp.guard.GuardAssembly.createNegotiationRecorder;
import static org.eclipse.edc.tasks.tck.dsp.guard.GuardAssembly.createNegotiationTriggers;
import static org.eclipse.edc.tasks.tck.dsp.guard.GuardAssembly.createTransferProcessRecorder;
import static org.eclipse.edc.tasks.tck.dsp.guard.GuardAssembly.createTransferProcessTriggers;

/**
 * Loads the transition guard.
 */
public class TckGuardExtension implements ServiceExtension {
    private static final String NAME = "DSP TCK Guard";

    @Inject
    private ContractNegotiationStore contractNegotiationStore;
    @Inject
    private TransferProcessStore transferProcessStore;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private EventRouter router;
    @Inject
    private Monitor monitor;

    @Inject
    private TaskService taskService;

    @Inject
    private Clock clock;

    private ContractNegotiationGuard negotiationGuard;
    private TransferProcessGuard transferProcessGuard;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public ContractNegotiationPendingGuard negotiationGuard() {

        var tasks = new ContractNegotiationGuardTask(taskService, clock);
        var recorder = createNegotiationRecorder(tasks);

        var registry = new StatefulEntityTriggerSubscriber<>(monitor.withPrefix("TckContractNegotiationTrigger"),
                contractNegotiationStore, transactionContext, ContractNegotiationEvent.class,
                ContractNegotiationEvent::getContractNegotiationId, 100);
        createNegotiationTriggers(tasks).forEach(registry::register);
        router.register(ContractNegotiationEvent.class, registry);

        negotiationGuard = new ContractNegotiationGuard(cn -> recorder.nextStep(cn.getContractOffers().get(0).getAssetId()), contractNegotiationStore);
        return negotiationGuard;

    }

    @Provider
    public TransferProcessPendingGuard transferProcessPendingGuard() {
        var tasks = new TransferProcessGuardTask(taskService, clock);

        var recorder = createTransferProcessRecorder(tasks);

        var registry = new StatefulEntityTriggerSubscriber<>(monitor.withPrefix("TckTransferProcessTrigger"),
                transferProcessStore, transactionContext, TransferProcessEvent.class,
                TransferProcessEvent::getTransferProcessId, 100);
        createTransferProcessTriggers(tasks).forEach(registry::register);
        router.register(TransferProcessEvent.class, registry);

        transferProcessGuard = new TransferProcessGuard(tp -> recorder.nextStep(tp.getContractId()), transferProcessStore);
        return transferProcessGuard;
    }

    @Override
    public void prepare() {
        if (negotiationGuard != null) {
            negotiationGuard.start();
        }
        if (transferProcessGuard != null) {
            transferProcessGuard.start();
        }
    }

    @Override
    public void shutdown() {
        if (negotiationGuard != null) {
            negotiationGuard.stop();
        }
        if (transferProcessGuard != null) {
            transferProcessGuard.stop();
        }
    }
}
