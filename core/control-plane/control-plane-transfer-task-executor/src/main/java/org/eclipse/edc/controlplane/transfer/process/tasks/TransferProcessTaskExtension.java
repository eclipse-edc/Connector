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

package org.eclipse.edc.controlplane.transfer.process.tasks;

import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessPendingGuard;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessors;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.controlplane.tasks.TaskService;
import org.eclipse.edc.controlplane.transfer.process.tasks.executor.TransferProcessTaskExecutorImpl;
import org.eclipse.edc.controlplane.transfer.process.tasks.listener.TransferProcessStateListener;
import org.eclipse.edc.controlplane.transfer.spi.TransferProcessTaskExecutor;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;

import static org.eclipse.edc.controlplane.transfer.process.tasks.TransferProcessTaskExtension.NAME;


@Extension(NAME)
public class TransferProcessTaskExtension implements ServiceExtension {

    public static final String NAME = "Transfer Task Extension";
    @Inject
    private TransferProcessStore store;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private TransferProcessPendingGuard pendingGuard;
    @Inject
    private Monitor monitor;
    @Inject
    private TaskService taskService;
    @Inject
    private TransferProcessors transferProcessors;
    @Inject
    private Clock clock;
    @Inject
    private TransferProcessObservable transferProcessObservable;

    @Override
    public void initialize(ServiceExtensionContext context) {
        transferProcessObservable.registerListener(new TransferProcessStateListener(taskService, clock));
    }

    @Provider
    public TransferProcessTaskExecutor transferProcessTaskExecutor() {
        return TransferProcessTaskExecutorImpl.Builder.newInstance()
                .store(store)
                .transactionContext(transactionContext)
                .monitor(monitor)
                .pendingGuard(pendingGuard)
                .taskService(taskService)
                .transferProcessors(transferProcessors)
                .clock(clock)
                .build();
    }

}
