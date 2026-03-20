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

package org.eclipse.edc.connector.controlplane.transfer;

import org.eclipse.edc.connector.controlplane.transfer.process.TransferProcessManagerImpl;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessPendingGuard;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessors;
import org.eclipse.edc.connector.controlplane.transfer.spi.retry.TransferWaitStrategy;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.statemachine.StateMachineConfiguration;

import java.time.Clock;

/**
 * Provides transfer manager service to the system.
 */
@Provides(TransferProcessManager.class)
@Extension(value = TransferManagerExtension.NAME)
public class TransferManagerExtension implements ServiceExtension {

    public static final String NAME = "Transfer Manager";

    @SettingContext("edc.transfer")
    @Configuration
    private StateMachineConfiguration stateMachineConfiguration;

    @Inject
    private TransferProcessStore transferProcessStore;
    @Inject
    private Clock clock;
    @Inject
    private Telemetry telemetry;
    @Inject
    private TransferProcessPendingGuard pendingGuard;
    @Inject
    private ExecutorInstrumentation executorInstrumentation;
    @Inject
    private TransferProcessors transferProcessors;

    private TransferProcessManagerImpl processManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var waitStrategy = context.hasService(TransferWaitStrategy.class) ? context.getService(TransferWaitStrategy.class) : stateMachineConfiguration.iterationWaitExponentialWaitStrategy();

        var entityRetryProcessConfiguration = stateMachineConfiguration.entityRetryProcessConfiguration();

        processManager = TransferProcessManagerImpl.Builder.newInstance()
                .waitStrategy(waitStrategy)
                .monitor(monitor)
                .telemetry(telemetry)
                .executorInstrumentation(executorInstrumentation)
                .clock(clock)
                .store(transferProcessStore)
                .batchSize(stateMachineConfiguration.batchSize())
                .entityRetryProcessConfiguration(entityRetryProcessConfiguration)
                .pendingGuard(pendingGuard)
                .transferProcessors(transferProcessors)
                .build();

        context.registerService(TransferProcessManager.class, processManager);
    }

    @Override
    public void start() {
        processManager.start();
    }

    @Override
    public void shutdown() {
        if (processManager != null) {
            processManager.stop();
        }
    }

}
