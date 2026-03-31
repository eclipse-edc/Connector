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

package org.eclipse.edc.controlplane.contract.negotiation.tasks;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.NegotiationProcessors;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.controlplane.contract.negotiation.tasks.executor.ContractNegotiationTaskExecutorImpl;
import org.eclipse.edc.controlplane.contract.negotiation.tasks.listener.ContractNegotiationStateListener;
import org.eclipse.edc.controlplane.contract.spi.negotiation.ContractNegotiationTaskExecutor;
import org.eclipse.edc.controlplane.tasks.TaskService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;

import static org.eclipse.edc.controlplane.contract.negotiation.tasks.ContractNegotiationTaskExtension.NAME;

@Extension(NAME)
public class ContractNegotiationTaskExtension implements ServiceExtension {

    public static final String NAME = "Contract Negotiation Task Extension";
    @Inject
    private ContractNegotiationStore contractNegotiationStore;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private ContractNegotiationPendingGuard pendingGuard;
    @Inject
    private Monitor monitor;
    @Inject
    private TaskService taskService;
    @Inject
    private NegotiationProcessors negotiationProcessors;
    @Inject
    private Clock clock;
    @Inject
    private ContractNegotiationObservable contractNegotiationObservable;


    @Override
    public void initialize(ServiceExtensionContext context) {
        contractNegotiationObservable.registerListener(new ContractNegotiationStateListener(taskService, clock));
    }

    @Provider
    public ContractNegotiationTaskExecutor contractNegotiationTaskExecutor() {
        return ContractNegotiationTaskExecutorImpl.Builder.newInstance()
                .taskService(taskService)
                .negotiationProcessors(negotiationProcessors)
                .clock(clock)
                .store(contractNegotiationStore)
                .transactionContext(transactionContext)
                .pendingGuard(pendingGuard)
                .monitor(monitor)
                .build();
    }
}
